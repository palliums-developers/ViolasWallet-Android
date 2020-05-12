package com.violas.wallet.biz

import com.palliums.utils.getDistinct
import com.palliums.utils.toMap
import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.BuildConfig
import com.violas.wallet.biz.governorApproval.ApprovalManager
import com.violas.wallet.biz.governorApproval.GovernorApprovalStatus
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO
import com.violas.wallet.repository.http.governor.GovernorInfoDTO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.governor.SSOApplicationMsgDTO
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/9 11:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class GovernorManager {

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }
    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mSSOApplicationMsgStorage by lazy {
        DataRepository.getSSOApplicationMsgStorage()
    }
    private val mSSOApplicationRecordStorage by lazy {
        DataRepository.getSSOApplicationRecordStorage()
    }
    private val mApprovalManager by lazy {
        ApprovalManager()
    }

    private var mNextMockGovernorApplicationStatus = -1

    /**
     * 注册州长
     */
    suspend fun signUpGovernor(
        accountDO: AccountDO,
        txid: String,
        toxid: String = ""
    ) {
        try {
            mGovernorService.signUpGovernor(
                accountDO.address,
                accountDO.publicKey,
                accountDO.walletNickname,
                txid,
                toxid
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 获取州长信息
     */
    suspend fun getGovernorInfo(
        accountDO: AccountDO
    ): GovernorInfoDTO = withContext(Dispatchers.IO) {
        val response =
            // test code =========> start
            try {
                mGovernorService.getGovernorInfo(accountDO.address)
            } catch (e: Exception) {
                if (BuildConfig.MOCK_GOVERNOR_DATA) {
                    Response<GovernorInfoDTO>()
                } else {
                    throw e
                }
            }
        // test code =========> end

        // test code =========> start
        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            response.data = GovernorInfoDTO(
                walletAddress = accountDO.address,
                name = accountDO.walletNickname,
                applicationStatus = mNextMockGovernorApplicationStatus,
                subAccountCount = 1
            )
            mNextMockGovernorApplicationStatus++
            if (mNextMockGovernorApplicationStatus == 5) {
                mNextMockGovernorApplicationStatus = -1
            }
        }
        // test code =========> end

        response.data ?: GovernorInfoDTO(
            walletAddress = accountDO.address,
            name = accountDO.walletNickname,
            applicationStatus = -1,
            subAccountCount = 1
        )
    }

    /**
     * publish 合约
     */
    suspend fun publishContract(
        account: Account
    ) = withContext(Dispatchers.IO) {
        try {
            val walletAddress = account.getAddress().toHex()

            // 1.检测合约是否publish，没有则先publish
            val published = mTokenManager.isPublishedContract(walletAddress)
            if (!published) {
                mTokenManager.publishContract(account)
            }

            // 2.通知服务器州长已publish合约
            mGovernorService.updateGovernorApplicationToPublished(walletAddress)
        } catch (e: Exception) {
            if (BuildConfig.MOCK_GOVERNOR_DATA) {
            } else {
                throw e
            }
        }
    }

    /**
     * 获取SSO申请消息
     */
    suspend fun getSSOApplicationMsgs(
        accountDO: AccountDO,
        pageSize: Int,
        offset: Int
    ): List<SSOApplicationMsgVO> = withContext(Dispatchers.IO) {
        val response =
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationMsgs(accountDO.address, pageSize, offset)
            } catch (e: Exception) {
                if (BuildConfig.MOCK_GOVERNOR_DATA) {
                    ListResponse<SSOApplicationMsgDTO>()
                } else {
                    throw e
                }
            }
        // test code =========> end

        // test code =========> start
        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            val fakeList = arrayListOf<SSOApplicationMsgDTO>()
            for (index in SSOApplicationState.CHAIRMAN_UNAPPROVED..SSOApplicationState.GOVERNOR_MINTED) {
                delay(200)
                var applicationStatus = System.currentTimeMillis()
                var expirationDate = applicationStatus
                if (index == SSOApplicationState.AUDIT_TIMEOUT) {
                    expirationDate -= 2 * 60 * 1000
                    applicationStatus = expirationDate - 5 * 24 * 60 * 60 * 1000
                } else {
                    expirationDate += 5 * 24 * 60 * 60 * 1000
                }

                fakeList.add(
                    SSOApplicationMsgDTO(
                        applicationId = "apply_id_$applicationStatus",
                        applicationStatus = index,
                        applicationDate = applicationStatus,
                        expirationDate = expirationDate,
                        applicantIdName = "Name $applicationStatus"
                    )
                )
            }

            response.data = fakeList
        }
        // test code =========> end

        if (response.data.isNullOrEmpty()) {
            return@withContext emptyList<SSOApplicationMsgVO>()
        }

        // 获取SSO发币申请本地记录
        val applicationIds = response.data!!.map { it.applicationId }.toTypedArray()
        val localMsgs =
            mSSOApplicationMsgStorage.loadMsgsFromApplicationIds(accountDO.id, *applicationIds)
                .toMap { it.applicationId }

        // DTO 转换 VO
        return@withContext response.data!!.map {
            val msgRead = when (it.applicationStatus) {
                SSOApplicationState.ISSUER_APPLYING -> {
                    localMsgs[it.applicationId]?.issueRead ?: false
                }

                SSOApplicationState.ISSUER_PUBLISHED -> {
                    localMsgs[it.applicationId]?.mintRead ?: false
                }

                else -> {
                    true
                }
            }

            SSOApplicationMsgVO(
                applicationId = it.applicationId,
                applicationStatus = it.applicationStatus,
                applicationDate = it.applicationDate,
                expirationDate = it.expirationDate,
                applicantIdName = it.applicantIdName,
                msgRead = msgRead
            )
        }
    }

    /**
     * 获取SSO申请详情
     */
    suspend fun getSSOApplicationDetails(
        accountDO: AccountDO,
        msgVO: SSOApplicationMsgVO
    ): SSOApplicationDetailsDTO? {
        // TODO delete test code
        val details =
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationDetails(accountDO.address, msgVO.applicationId)
            } catch (e: Exception) {
                if (BuildConfig.MOCK_GOVERNOR_DATA) {
                    SSOApplicationDetailsDTO(
                        applicationId = msgVO.applicationId,
                        applicationStatus = msgVO.applicationStatus,
                        issuerWalletAddress = "f4174e9eabcb2e968e22da4c75ac653b",
                        idName = msgVO.applicantIdName,
                        idNumber = "1234567890",
                        idPhotoPositiveUrl = "",
                        idPhotoBackUrl = "",
                        countryCode = "CN",
                        countryName = "中国",
                        emailAddress = "luckeast@163.com",
                        phoneNumber = "18919025675",
                        phoneAreaCode = "+86",
                        fiatCurrencyType = "RMB",
                        tokenAmount = "100000000000000",
                        tokenName = "DTY",
                        tokenValue = 2,
                        tokenIdx = 1,
                        reservePhotoUrl = "http://52.27.228.84:4000/1.0/violas/photo/202005061016156252.jpeg",
                        bankChequePhotoPositiveUrl = "http://52.27.228.84:4000/1.0/violas/photo/202005090243496532.jpeg",
                        bankChequePhotoBackUrl = "http://52.27.228.84:4000/1.0/violas/photo/202005061016148281.jpeg",
                        applicationDate = msgVO.applicationDate,
                        applicationPeriod = 5,
                        expirationDate = msgVO.expirationDate,
                        unapprovedReason =
                        if (msgVO.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED
                            || msgVO.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED
                        ) {
                            "信息不完善"
                        } else {
                            null
                        }
                    )
                } else {
                    throw e
                }
            }
        // test code =========> end

        details?.let {
            if (msgVO.applicationStatus != it.applicationStatus) {
                msgVO.applicationStatus = it.applicationStatus
                updateSSOApplicationMsgStatus(accountDO.id, msgVO)
            }
        }

        return details
    }

    /**
     * 获取审核不通过SSO申请原因列表
     */
    suspend fun getUnapproveReasons() =
        try {
            mGovernorService.getUnapproveReasons()
        } catch (e: Exception) {
            if (BuildConfig.MOCK_GOVERNOR_DATA) {
                arrayListOf(
                    UnapproveReasonDTO(1, "信息不完善"),
                    UnapproveReasonDTO(-1, "其他")
                )
            } else {
                throw e
            }
        }

    /**
     * 审核不通过SSO申请
     */
    suspend fun unapproveSSOApplication(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        reasonType: Int,
        reasonRemarks: String = ""
    ) {
        try {
            mGovernorService.unapproveSSOApplication(
                ssoApplicationId = ssoApplicationDetails.applicationId,
                issuerWalletAddress = ssoApplicationDetails.issuerWalletAddress,
                reasonType = reasonType,
                reasonRemarks = reasonRemarks
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 审核通过SSO申请
     * 审核通过SSO申请时，州长先向董事长申请发行币种，待董事长发行币种完成后，州长在转账并通知发行商
     */
    suspend fun approveSSOApplication(
        details: SSOApplicationDetailsDTO
    ) {
        try {
            mGovernorService.submitSSOApplicationApprovalResults(
                ssoApplicationId = details.applicationId,
                issuerWalletAddress = details.issuerWalletAddress,
                approvalResults = SSOApplicationState.GOVERNOR_APPROVED
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 转平台币给发行商
     */
    suspend fun transferCoinToIssuer(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        account: Account? = null,
        walletAddress: String
    ) {
        try {
            mApprovalManager.transferCoinToIssuer(
                account = account,
                walletAddress = walletAddress,
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.issuerWalletAddress
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 铸稳定币给发行商
     */
    suspend fun mintTokenToIssuer(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        account: Account
    ) {
        try {
            mApprovalManager.mintTokenToIssuer(
                account = account,
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.issuerWalletAddress,
                ssoApplyAmount = ssoApplicationDetails.tokenAmount.toLong(),
                newTokenIdx = ssoApplicationDetails.tokenIdx!!
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    fun isTransferredCoinToIssuer(walletAddress: String, applicationId: String): Boolean {
        val record = mSSOApplicationRecordStorage.find(walletAddress, applicationId)
        return record?.status ?: GovernorApprovalStatus.NONE >= GovernorApprovalStatus.TRANSFERRED
    }

    fun getSSOApplicationMsgLiveData(
        accountId: Long,
        applicationId: String
    ) =
        mSSOApplicationMsgStorage.loadMsgLiveDataFromApplicationId(accountId, applicationId)
            .getDistinct()

    /**
     * 标记SSO申请消息为已读
     */
    fun markSSOApplicationMsgAsRead(
        accountId: Long,
        applicationId: String,
        @SSOApplicationState
        applicationStatus: Int,
        applicationDate: Long,
        expirationDate: Long,
        applicantIdName: String
    ) {
        try {
            val msgDO =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    accountId,
                    applicationId
                )

            if (msgDO == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = accountId,
                        applicationId = applicationId,
                        applicationStatus = applicationStatus,
                        applicationDate = applicationDate,
                        expirationDate = expirationDate,
                        applicantIdName = applicantIdName,
                        issueRead = true,
                        mintRead = applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED
                    )
                )
            } else if (applicationStatus != msgDO.applicationStatus) {
                msgDO.applicationStatus = applicationStatus
                if (applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED) {
                    msgDO.mintRead = true
                }
                mSSOApplicationMsgStorage.update(msgDO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 标记SSO申请消息为已读
     */
    fun markSSOApplicationMsgAsRead(accountId: Long, msgVO: SSOApplicationMsgVO) {
        try {
            val msgDO =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    accountId,
                    msgVO.applicationId
                )

            if (msgDO == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = accountId,
                        applicationId = msgVO.applicationId,
                        applicationStatus = msgVO.applicationStatus,
                        applicationDate = msgVO.applicationDate,
                        expirationDate = msgVO.expirationDate,
                        applicantIdName = msgVO.applicantIdName,
                        issueRead = true,
                        mintRead = msgVO.applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED
                    )
                )
            } else if (msgVO.applicationStatus != msgDO.applicationStatus) {
                msgDO.applicationStatus = msgVO.applicationStatus
                if (msgVO.applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED) {
                    msgDO.mintRead = true
                }
                mSSOApplicationMsgStorage.update(msgDO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新SSO申请消息状态
     */
    fun updateSSOApplicationMsgStatus(accountId: Long, msgVO: SSOApplicationMsgVO) {
        try {
            val msgDO =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    accountId,
                    msgVO.applicationId
                )

            if (msgDO == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = accountId,
                        applicationId = msgVO.applicationId,
                        applicationDate = msgVO.applicationDate,
                        applicationStatus = msgVO.applicationStatus,
                        applicantIdName = msgVO.applicantIdName,
                        expirationDate = msgVO.expirationDate,
                        issueRead = true,
                        mintRead = msgVO.applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED
                    )
                )
            } else {
                msgDO.applicationStatus = msgVO.applicationStatus
                msgDO.mintRead = msgVO.applicationStatus >= SSOApplicationState.ISSUER_PUBLISHED
                mSSOApplicationMsgStorage.update(msgDO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
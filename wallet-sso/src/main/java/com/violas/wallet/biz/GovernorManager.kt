package com.violas.wallet.biz

import com.palliums.net.RequestException
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
import com.violas.wallet.ui.selectCountryArea.getCountryArea
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
            for (index in SSOApplicationState.CHAIRMAN_UNAPPROVED..SSOApplicationState.MINTED_TOKEN) {
                delay(200)
                var applicationStatus = System.currentTimeMillis()
                var expirationDate = applicationStatus
                if (index == SSOApplicationState.APPROVAL_TIMEOUT) {
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
                SSOApplicationState.APPLYING_ISSUE_TOKEN -> {
                    localMsgs[it.applicationId]?.issueRead ?: false
                }

                SSOApplicationState.APPLYING_MINT_TOKEN -> {
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
        val response =
        // TODO delete test code
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationDetails(msgVO.applicationId)
            } catch (e: Exception) {
                if (BuildConfig.MOCK_GOVERNOR_DATA) {
                    Response<SSOApplicationDetailsDTO>()
                } else {
                    throw e
                }
            }
        // test code =========> end

        // test code =========> start
        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            val fakeDetails = SSOApplicationDetailsDTO(
                ssoWalletAddress = "f4174e9eabcb2e968e22da4c75ac653b",
                idName = msgVO.applicantIdName,
                idNumber = "1234567890",
                idPhotoPositiveUrl = "",
                idPhotoBackUrl = "",
                countryCode = "CN",
                emailAddress = "luckeast@163.com",
                phoneNumber = "18919025675",
                phoneAreaCode = "+86",
                fiatCurrencyType = "RMB",
                tokenAmount = "100000000000000",
                tokenName = "DTY",
                tokenValue = 2,
                tokenIdx = 1,
                reservePhotoUrl = "",
                bankChequePhotoPositiveUrl = "",
                bankChequePhotoBackUrl = "",
                applicationDate = msgVO.applicationDate,
                applicationPeriod = 5,
                expirationDate = msgVO.expirationDate,
                applicationStatus = msgVO.applicationStatus,
                applicationId = msgVO.applicationId,
                unapprovedReason =
                if (msgVO.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED
                    || msgVO.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED
                ) {
                    "信息不完善"
                } else {
                    null
                }
            )
            response.data = fakeDetails
        }
        // test code =========> end

        if (response.data != null) {
            if (response.data!!.applicationStatus >= SSOApplicationState.GIVEN_MINTABLE
                && response.data!!.tokenIdx == null
            ) {
                throw RequestException.responseDataException(
                    "token id cannot be null"
                )
            } else if ((response.data!!.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED
                        || response.data!!.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED)
                && response.data!!.unapprovedReason.isNullOrEmpty()
            ) {
                throw RequestException.responseDataException(
                    "unapproved reason cannot be null"
                )
            }

            val countryArea = getCountryArea(response.data!!.countryCode)
            response.data!!.countryName = countryArea.countryName

            if (msgVO.applicationStatus != response.data!!.applicationStatus) {
                msgVO.applicationStatus = response.data!!.applicationStatus
                updateSSOApplicationMsgStatus(accountDO.id, msgVO)
            }
        }

        return response.data
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
        reasonRemark: String = ""
    ) {
        try {
            mGovernorService.unapproveSSOApplication(
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.ssoWalletAddress,
                reasonType = reasonType,
                reasonRemark = reasonRemark
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 申请铸币权
     * 审核通过SSO申请时，州长先向董事长申请发行币种，待董事长发行币种完成后，州长在转账并通过SSO申请
     */
    suspend fun applyForMintable(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        walletAddress: String
    ) {
        try {
            mGovernorService.applyForMintable(
                walletAddress = walletAddress,
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.ssoWalletAddress
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 审核通过SSO申请
     */
    suspend fun approveSSOApplication(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        account: Account? = null,
        walletAddress: String
    ) {
        try {
            mApprovalManager.approve(
                account = account,
                walletAddress = walletAddress,
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.ssoWalletAddress
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    /**
     * 给SSO账户铸币
     */
    suspend fun mintTokenToSSOAccount(
        ssoApplicationDetails: SSOApplicationDetailsDTO,
        account: Account
    ) {
        try {
            mApprovalManager.mint(
                account = account,
                ssoApplicationId = ssoApplicationDetails.applicationId,
                ssoWalletAddress = ssoApplicationDetails.ssoWalletAddress,
                ssoApplyAmount = ssoApplicationDetails.tokenAmount.toLong(),
                newTokenIdx = ssoApplicationDetails.tokenIdx!!
            )
        } catch (e: Exception) {
            if (!BuildConfig.MOCK_GOVERNOR_DATA) {
                throw e
            }
        }
    }

    fun isTransferredCoinToSSO(walletAddress: String, applicationId: String): Boolean {
        val record = mSSOApplicationRecordStorage.find(walletAddress, applicationId)
        return record?.status ?: 0 >= GovernorApprovalStatus.ReadyApproval
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
                        mintRead = applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN
                    )
                )
            } else if (applicationStatus != msgDO.applicationStatus) {
                msgDO.applicationStatus = applicationStatus
                if (applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN) {
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
                        mintRead = msgVO.applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN
                    )
                )
            } else if (msgVO.applicationStatus != msgDO.applicationStatus) {
                msgDO.applicationStatus = msgVO.applicationStatus
                if (msgVO.applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN) {
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
                        mintRead = msgVO.applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN
                    )
                )
            } else {
                msgDO.applicationStatus = msgVO.applicationStatus
                msgDO.mintRead = msgVO.applicationStatus >= SSOApplicationState.APPLYING_MINT_TOKEN
                mSSOApplicationMsgStorage.update(msgDO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
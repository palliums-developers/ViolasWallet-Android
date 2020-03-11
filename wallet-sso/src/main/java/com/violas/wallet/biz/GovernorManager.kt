package com.violas.wallet.biz

import android.content.Context
import com.palliums.net.RequestException
import com.palliums.utils.getDistinct
import com.palliums.utils.getString
import com.palliums.utils.isNetworkConnected
import com.palliums.utils.toMap
import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.biz.applysso.ApplySSOManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO
import com.violas.wallet.repository.http.governor.GovernorInfoDTO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.governor.SSOApplicationMsgDTO
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.Account
import java.util.concurrent.CountDownLatch

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
    private val mSSOApplicationMsgStorage by lazy {
        DataRepository.getSSOApplicationMsgStorage()
    }
    private val mApplySSOManager by lazy {
        ApplySSOManager()
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
        mGovernorService.signUpGovernor(accountDO.address, accountDO.walletNickname, txid, toxid)
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
     * 注册VStake
     */
    suspend fun publishVStake(
        context: Context,
        account: Account
    ) = withContext(Dispatchers.IO) {
        // 1.获取VStake address
        val vStakeAddress = try {
            mGovernorService.getVStakeAddress().data?.address!!
        } catch (e: Exception) {
            if (BuildConfig.MOCK_GOVERNOR_DATA) {
                account.getAddress().toHex()
            } else {
                throw e
            }
        }

        // 2.publish VStake
        var result = false
        val countDownLatch = CountDownLatch(1)
        DataRepository.getViolasService()
            .publishToken(context, account, vStakeAddress) {
                result = it
                countDownLatch.countDown()
            }
        countDownLatch.await()

        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            result = true
        }

        if (!result) {
            throw if (!isNetworkConnected())
                RequestException.networkUnavailable()
            else
                RequestException(
                    RequestException.ERROR_CODE_UNKNOWN_ERROR,
                    getString(R.string.tips_apply_for_licence_fail)
                )
        }

        // 3.通知服务器州长已publish VStake
        try {
            mGovernorService.updateGovernorApplicationToPublished(account.getAddress().toHex())
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
            for (index in 0..5) {
                delay(200)
                val date = System.currentTimeMillis()
                fakeList.add(
                    SSOApplicationMsgDTO(
                        applicationId = "apply_id_$date",
                        applicationStatus = if (index > 4) 1 else index,
                        applicationDate = date,
                        expirationDate = if (index > 4) date - 1000 else date + 1000,
                        applicantIdName = "Name $date"
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
        val localMsgs = mSSOApplicationMsgStorage
            .loadMsgsFromApplicationIds(accountDO.id, *applicationIds)
            .toMap { it.applicationId }

        // DTO 转换 VO
        return@withContext response.data!!.map {
            val msgUnread = when (it.applicationStatus) {
                0 -> { // not approved
                    val ssoRecord = localMsgs[it.applicationId]
                    ssoRecord == null || ssoRecord.issuingUnread
                }

                3 -> { // published
                    val ssoRecord = localMsgs[it.applicationId]
                    ssoRecord == null || ssoRecord.mintUnread
                }

                else -> {
                    false
                }
            }

            SSOApplicationMsgVO(
                applicationId = it.applicationId,
                applicationDate = it.applicationDate,
                applicationStatus = it.applicationStatus,
                applicantIdName = it.applicantIdName,
                msgUnread = msgUnread
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
                ssoWalletAddress = accountDO.address,
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
                tokenAddress = accountDO.address,
                reservePhotoUrl = "",
                bankChequePhotoPositiveUrl = "",
                bankChequePhotoBackUrl = "",
                applicationDate = msgVO.applicationDate,
                applicationPeriod = 7,
                expirationDate = System.currentTimeMillis(),
                applicationStatus = msgVO.applicationStatus,
                walletLayersNumber = 2,
                applicationId = msgVO.applicationId
            )
            response.data = fakeDetails
        }
        // test code =========> end

        if (response.data != null) {
            if (response.data!!.applicationStatus == 1
                || response.data!!.applicationStatus >= 3
            ) {
                if (response.data!!.tokenAddress.isNullOrEmpty()) {
                    throw RequestException.responseDataException(
                        "module address cannot be null"
                    )
                } else if (response.data!!.walletLayersNumber <= 0) {
                    throw RequestException.responseDataException(
                        "Incorrect module depth(${response.data!!.walletLayersNumber})"
                    )
                }
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
     * 审批SSO申请通过
     */
    suspend fun passSSOApplication(
        ssoWalletAddress: String,
        account: Account,
        mnemonics: List<String>
    ) {
        var result = mApplySSOManager.apply(
            applySSOWalletAddress = ssoWalletAddress,
            account = account,
            mnemonic = mnemonics
        )

        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            result = true
        }

        if (!result) {
            throw if (!isNetworkConnected())
                RequestException.networkUnavailable()
            else
                RequestException(
                    RequestException.ERROR_CODE_UNKNOWN_ERROR,
                    getString(R.string.tips_governor_approval_fail)
                )
        }
    }

    /**
     * 审批SSO申请不通过
     */
    suspend fun rejectSSOApplication(ssoWalletAddress: String) {
        try {
            mGovernorService.approvalSSOApplication(
                pass = false,
                newTokenAddress = "",
                ssoWalletAddress = ssoWalletAddress,
                walletLayersNumber = -1
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
        account: Account,
        mnemonics: List<String>
    ) {
        var result = mApplySSOManager.mint(
            account = account,
            mnemonic = mnemonics,
            walletLayersNumber = ssoApplicationDetails.walletLayersNumber,
            tokenAddress = ssoApplicationDetails.tokenAddress!!,
            receiveAddress = ssoApplicationDetails.ssoWalletAddress,
            receiveAmount = ssoApplicationDetails.tokenAmount.toLong()
        )

        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            result = true
        }

        if (!result) {
            throw if (!isNetworkConnected())
                RequestException.networkUnavailable()
            else
                RequestException(
                    RequestException.ERROR_CODE_UNKNOWN_ERROR,
                    getString(R.string.tips_governor_mint_token_fail)
                )
        }
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
                        applicationDate = msgVO.applicationDate,
                        applicationStatus = msgVO.applicationStatus,
                        applicantIdName = msgVO.applicantIdName,
                        issuingUnread = false,
                        mintUnread = msgVO.applicationStatus < 3
                    )
                )
            } else if (msgVO.applicationStatus != msgDO.applicationStatus) {
                msgDO.applicationStatus = msgVO.applicationStatus
                if (msgVO.applicationStatus < 3) {
                    msgDO.mintUnread = false
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
                        issuingUnread = false,
                        mintUnread = msgVO.applicationStatus < 3
                    )
                )
            } else {
                msgDO.applicationStatus = msgVO.applicationStatus
                msgDO.mintUnread = msgVO.applicationStatus < 3
                mSSOApplicationMsgStorage.update(msgDO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
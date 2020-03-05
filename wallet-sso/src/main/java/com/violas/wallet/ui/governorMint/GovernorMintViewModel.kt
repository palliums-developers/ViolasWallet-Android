package com.violas.wallet.ui.governorMint

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.net.RequestException
import com.palliums.violas.http.Response
import com.violas.wallet.BuildConfig
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.applysso.ApplySSOManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class GovernorMintViewModelFactory(
    private val mSSOApplicationMsg: SSOApplicationMsgVO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationMsgVO::class.java)
            .newInstance(mSSOApplicationMsg)
    }
}

class GovernorMintViewModel(
    private val mSSOApplicationMsg: SSOApplicationMsgVO
) : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01
        const val ACTION_MINT_TOKEN = 0x02
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }
    private val mSSOApplicationMsgStorage by lazy {
        DataRepository.getSSOApplicationMsgStorage()
    }
    private val mApplySSOManager by lazy {
        ApplySSOManager()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)

            // 进入州长铸币页面自动标记本地消息为已读
            markLocalMsgAsRead(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        if (action == ACTION_LOAD_APPLICATION_DETAILS) {
            // 加载申请详情
            loadApplicationDetails()
            return
        }

        // 给SSO申请者铸币
        mApplySSOManager.mint(
            account = params[0] as Account,
            mnemonic = params[1] as List<String>,
            walletLayersNumber = mSSOApplicationDetailsLD.value!!.walletLayersNumber,
            tokenAddress = mSSOApplicationDetailsLD.value!!.tokenAddress!!,
            receiveAddress = mSSOApplicationDetailsLD.value!!.ssoWalletAddress,
            receiveAmount = mSSOApplicationDetailsLD.value!!.tokenAmount.toLong()
        )

        // 铸币成功后更新本地消息状态
        updateLocalMsgStatus(4)
    }

    private suspend fun loadApplicationDetails() {
        val response =
        // TODO delete test code
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationDetails(mSSOApplicationMsg.applicationId)
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
                ssoWalletAddress = mAccountLD.value!!.address,
                idName = mSSOApplicationMsg.applicantIdName,
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
                tokenAddress = mAccountLD.value!!.address,
                reservePhotoUrl = "",
                bankChequePhotoPositiveUrl = "",
                bankChequePhotoBackUrl = "",
                applicationDate = mSSOApplicationMsg.applicationDate,
                applicationPeriod = 7,
                expirationDate = System.currentTimeMillis(),
                applicationStatus = mSSOApplicationMsg.applicationStatus,
                walletLayersNumber = 2,
                applicationId = mSSOApplicationMsg.applicationId
            )
            response.data = fakeDetails
        }
        // test code =========> end

        if (response.data != null) {
            if (response.data!!.applicationStatus == 1 || response.data!!.applicationStatus >= 3) {
                if (response.data!!.tokenAddress.isNullOrEmpty()) {
                    throw RequestException.responseDataException("module address cannot be null")
                } else if (response.data!!.walletLayersNumber <= 0) {
                    throw RequestException.responseDataException(
                        "Incorrect module depth(${response.data!!.walletLayersNumber})"
                    )
                }
            }

            val countryArea = getCountryArea(response.data!!.countryCode)
            response.data!!.countryName = countryArea.countryName

            if (mSSOApplicationMsg.applicationStatus != response.data!!.applicationStatus) {
                updateLocalMsgStatus(response.data!!.applicationStatus)
            }
        }
        mSSOApplicationDetailsLD.postValue(response.data)
    }

    /**
     * 标记本地消息为已读
     */
    private fun markLocalMsgAsRead(accountDO: AccountDO) {
        try {
            val localMsg =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    accountDO.id,
                    mSSOApplicationMsg.applicationId
                )
            if (localMsg == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = accountDO.id,
                        applicationId = mSSOApplicationMsg.applicationId,
                        applicationDate = mSSOApplicationMsg.applicationDate,
                        applicationStatus = mSSOApplicationMsg.applicationStatus,
                        applicantIdName = mSSOApplicationMsg.applicantIdName,
                        issuingUnread = false,
                        mintUnread = mSSOApplicationMsg.applicationStatus < 3
                    )
                )
            } else if (mSSOApplicationMsg.applicationStatus != localMsg.applicationStatus) {
                localMsg.applicationStatus = mSSOApplicationMsg.applicationStatus
                if (mSSOApplicationMsg.applicationStatus < 3) {
                    localMsg.mintUnread = false
                }
                mSSOApplicationMsgStorage.update(localMsg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新本地消息状态
     */
    private fun updateLocalMsgStatus(targetApplicationStatus: Int) {
        try {
            val localMsg =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    mAccountLD.value!!.id,
                    mSSOApplicationMsg.applicationId
                )

            if (localMsg == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = mAccountLD.value!!.id,
                        applicationId = mSSOApplicationMsg.applicationId,
                        applicationDate = mSSOApplicationMsg.applicationDate,
                        applicationStatus = targetApplicationStatus,
                        applicantIdName = mSSOApplicationMsg.applicantIdName,
                        issuingUnread = false,
                        mintUnread = targetApplicationStatus < 3
                    )
                )
            } else {
                localMsg.applicationStatus = targetApplicationStatus
                localMsg.mintUnread = targetApplicationStatus < 3
                mSSOApplicationMsgStorage.update(localMsg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
package com.violas.wallet.ui.governorApproval

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.violas.http.Response
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
class SSOApplicationDetailsViewModelFactory(
    private val mSSOApplicationMsg: SSOApplicationMsgVO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationMsgVO::class.java)
            .newInstance(mSSOApplicationMsg)
    }
}

class SSOApplicationDetailsViewModel(
    private val mSSOApplicationMsg: SSOApplicationMsgVO
) : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01
        const val ACTION_APPROVAL_APPLICATION = 0x02
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

            // 存储SSO申请消息
            val localMsg =
                mSSOApplicationMsgStorage.loadMsgFromApplicationId(
                    currentAccount.id,
                    mSSOApplicationMsg.applicationId
                )
            if (localMsg == null) {
                mSSOApplicationMsgStorage.insert(
                    SSOApplicationMsgDO(
                        accountId = currentAccount.id,
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
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        if (action == ACTION_LOAD_APPLICATION_DETAILS) {
            // 加载申请详情
            loadApplicationDetails()
            return
        }

        // 审批申请
        val pass = params[0] as Boolean
        val ssoWalletAddress = mSSOApplicationDetailsLD.value!!.ssoWalletAddress
        if (!pass) {
            // 审核不通过
            mGovernorService.approvalSSOApplication(
                pass = false,
                newTokenAddress = "",
                ssoWalletAddress = ssoWalletAddress,
                walletLayersNumber = -1
            )
            return
        }

        // 审核通过
        val account = params[1] as Account
        val mnemonics = params[2] as List<String>
        mApplySSOManager.apply(account, ssoWalletAddress, mnemonics)
    }

    private suspend fun loadApplicationDetails() {
        val response =
        // TODO delete test code
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationDetails(mSSOApplicationMsg.applicationId)
            } catch (e: Exception) {
                Response<SSOApplicationDetailsDTO>()
            }

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
            tokenAddress = "",
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
        // test code =========> end

        if (response.data != null) {
            val countryArea = getCountryArea(response.data!!.countryCode)
            response.data!!.countryName = countryArea.countryName
        }
        mSSOApplicationDetailsLD.postValue(response.data)
    }
}
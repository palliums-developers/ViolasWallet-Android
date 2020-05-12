package com.violas.wallet.ui.verification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.BindEmailEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.local.user.AccountBindingStatus
import com.violas.wallet.repository.local.user.EmailInfo
import com.violas.wallet.utils.validationEmailAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2019-11-25 17:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class EmailVerificationViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_VERIFICATION_CODE = 0
        const val ACTION_BING_EMAIL = 1
    }

    private lateinit var currentAccount: AccountDO

    private val issuerService by lazy {
        DataRepository.getIssuerService()
    }

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    val getVerificationCodeResult = MutableLiveData<Boolean>()
    val bindEmailResult = MutableLiveData<Boolean>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            currentAccount = AccountManager().currentAccount()
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val walletAddress = currentAccount.address
        val emailAddress = params[0] as String

        // 获取验证码操作
        if (action == ACTION_GET_VERIFICATION_CODE) {
            issuerService.sendEmailVerifyCode(walletAddress, emailAddress)

            tipsMessage.postValueSupport(getString(R.string.hint_verification_code_get_success))
            getVerificationCodeResult.postValue(true)
            return
        }

        // 绑定邮箱操作
        val verificationCode = params[1] as String
        issuerService.bindEmail(walletAddress, emailAddress, verificationCode)

        val emailInfo = EmailInfo(emailAddress, AccountBindingStatus.BOUND)
        localUserService.setEmailInfo(emailInfo)
        EventBus.getDefault().post(BindEmailEvent(emailInfo))

        bindEmailResult.postValue(true)
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) {
            return false
        }

        val emailAddress = params[0] as String
        if (emailAddress.isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.hint_enter_email_address))
            return false
        }

        if (!validationEmailAddress(emailAddress)) {
            tipsMessage.postValueSupport(getString(R.string.hint_email_format_incorrect))
            return false
        }

        if (action == ACTION_BING_EMAIL && (params[1] as String).isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.hint_enter_verification_code))
            return false
        }

        return true
    }
}
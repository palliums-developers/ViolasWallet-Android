package com.violas.wallet.ui.verification

import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.event.BindEmailEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.local.user.EmailInfo
import com.violas.wallet.utils.validationEmailAddress
import kotlinx.coroutines.delay
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

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    val getVerificationCodeResult = MutableLiveData<Boolean>()
    val bindEmailResult = MutableLiveData<Boolean>()

    override suspend fun realExecute(
        action: Int,
        vararg params: Any,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口

        // test code
        delay(3000)

        val emailAddress = params[0] as String

        if (action == ACTION_GET_VERIFICATION_CODE) {
            // 获取验证码操作
            tipsMessage.postValue(getString(R.string.hint_verification_code_get_success))
            getVerificationCodeResult.postValue(true)
            onSuccess.invoke()
            return
        }

        // 绑定邮箱操作
        tipsMessage.postValue(getString(R.string.hint_email_bind_success))
        bindEmailResult.postValue(true)

        val emailInfo = EmailInfo(emailAddress)
        localUserService.setEmailInfo(emailInfo)
        EventBus.getDefault().post(BindEmailEvent(emailInfo))

        onSuccess.invoke()
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) {
            return false
        }

        val emailAddress = params[0] as String
        if (emailAddress.isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_email_address))
            return false
        }

        if (!validationEmailAddress(emailAddress)) {
            tipsMessage.postValue(getString(R.string.hint_email_format_incorrect))
            return false
        }

        if (action == ACTION_BING_EMAIL && (params[1] as String).isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_verification_code))
            return false
        }

        return true
    }
}
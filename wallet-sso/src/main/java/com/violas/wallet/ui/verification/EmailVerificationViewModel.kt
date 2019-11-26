package com.violas.wallet.ui.verification

import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.utils.validationEmailAddress
import kotlinx.coroutines.delay

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

        if (action == ACTION_BING_EMAIL) {
            // 绑定邮箱操作
            tipsMessage.postValue(getString(R.string.hint_email_bind_success))
            bindEmailResult.postValue(true)
            onSuccess.invoke()
            return
        }

        // 获取验证码操作
        tipsMessage.postValue(getString(R.string.hint_verification_code_get_success))
        getVerificationCodeResult.postValue(true)
        onSuccess.invoke()
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) {
            return false
        }

        val phoneNumber = params[0] as String
        if (phoneNumber.isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_email_address))
            return false
        }

        if (!validationEmailAddress(phoneNumber)) {
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
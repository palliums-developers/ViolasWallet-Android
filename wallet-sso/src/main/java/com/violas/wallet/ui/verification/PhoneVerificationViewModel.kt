package com.violas.wallet.ui.verification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.utils.validationChinaPhone
import com.violas.wallet.utils.validationHkPhone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2019-11-25 14:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class PhoneVerificationViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_VERIFICATION_CODE = 0
        const val ACTION_BING_PHONE_NUMBER = 1
    }

    val getVerificationCodeResult = MutableLiveData<Boolean>()
    val bindPhoneNumberResult = MutableLiveData<Boolean>()
    val countryAreaVO = MutableLiveData<CountryAreaVO>()

    fun loadCountryArea() {
        viewModelScope.launch {
            val countryArea = getCountryArea()
            countryAreaVO.postValue(countryArea)
        }
    }

    override suspend fun realExecute(
        action: Int,
        vararg params: Any,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口

        // test code
        delay(3000)

        if (action == ACTION_BING_PHONE_NUMBER) {
            // 绑定手机号操作
            tipsMessage.postValue(getString(R.string.hint_phone_number_bind_success))
            bindPhoneNumberResult.postValue(true)
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
            tipsMessage.postValue(getString(R.string.hint_enter_phone_number))
            return false
        }

        val countryAreaVO = countryAreaVO.value ?: return false
        if (isChinaMainland(countryAreaVO)) {
            if (!validationChinaPhone(phoneNumber)) {
                tipsMessage.postValue(getString(R.string.hint_phone_number_format_incorrect))
                return false
            }
        } else {
            if (!validationHkPhone(phoneNumber)) {
                tipsMessage.postValue(getString(R.string.hint_phone_number_format_incorrect))
                return false
            }
        }

        if (action == ACTION_BING_PHONE_NUMBER && (params[1] as String).isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_verification_code))
            return false
        }

        return true
    }
}
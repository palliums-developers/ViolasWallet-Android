package com.violas.wallet.ui.verification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.BindPhoneEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.local.user.PhoneInfo
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.utils.validationChinaPhone
import com.violas.wallet.utils.validationHkPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

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

    private lateinit var currentAccount: AccountDO

    private val ssoService by lazy {
        DataRepository.getSSOService()
    }

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    val getVerificationCodeResult = MutableLiveData<Boolean>()
    val bindPhoneNumberResult = MutableLiveData<Boolean>()
    val countryAreaVO = MutableLiveData<CountryAreaVO>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            currentAccount = AccountManager().currentAccount()

            val countryArea = getCountryArea()
            countryAreaVO.postValue(countryArea)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val areaCode = "${countryAreaVO.value!!.areaCode}"
        val phoneNumber = params[0] as String
        val walletAddress = currentAccount.address

        // 获取验证码操作
        if (action == ACTION_GET_VERIFICATION_CODE) {
            ssoService.sendPhoneVerifyCode(walletAddress, phoneNumber, areaCode)

            tipsMessage.postValue(getString(R.string.hint_verification_code_get_success))
            getVerificationCodeResult.postValue(true)
            return
        }

        // 绑定手机号操作
        val verificationCode = params[1] as String
        ssoService.bindPhone(walletAddress, phoneNumber, areaCode, verificationCode)

        val phoneInfo = PhoneInfo(areaCode, phoneNumber)
        localUserService.setPhoneInfo(phoneInfo)
        EventBus.getDefault().post(BindPhoneEvent(phoneInfo))

        tipsMessage.postValue(getString(R.string.hint_phone_number_bind_success))
        bindPhoneNumberResult.postValue(true)
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
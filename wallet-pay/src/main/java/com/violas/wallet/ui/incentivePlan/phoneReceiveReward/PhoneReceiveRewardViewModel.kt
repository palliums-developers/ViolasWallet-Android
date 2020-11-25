package com.violas.wallet.ui.incentivePlan.phoneReceiveReward

import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.utils.validationViolasAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 11/25/20 4:55 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class PhoneReceiveRewardViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_VERIFICATION_CODE = 0
        const val ACTION_RECEIVE_REWARD = 1
    }

    private var violasAccount: AccountDO? = null
    val countryAreaLiveData = MutableLiveData<CountryAreaVO>()

    suspend fun init() = withContext(Dispatchers.IO) {
        val countryArea = getCountryArea()
        countryAreaLiveData.postValue(countryArea)

        try {
            violasAccount =
                DataRepository.getAccountStorage().findByCoinType(CoinTypes.Violas.coinType())
        } catch (ignore: Exception) {

        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val areaCode = countryAreaLiveData.value!!.areaCode
        val phoneNumber = params[0] as String
        val walletAddress = violasAccount!!.address

        // 获取验证码操作
        if (action == ACTION_GET_VERIFICATION_CODE) {
            // TODO 发送获取验证码请求
            delay(3000)
            tipsMessage.postValueSupport(getString(R.string.hint_get_verification_code_success))
            return
        }

        // 领取奖励
        // TODO 领取逻辑
        val verificationCode = params[1] as String
        val inviterAddress = params[2] as String
        delay(3000)
        tipsMessage.postValueSupport(getString(R.string.hint_phone_receive_reward_success))
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) return false

        val countryArea = countryAreaLiveData.value ?: return false
        if (violasAccount == null) {
            tipsMessage.postValueSupport(getString(R.string.hint_account_error))
            return false
        }

        val phoneNumber = params[0] as String
        if (phoneNumber.isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.hint_input_phone_number))
            return false
        }

        if (action == ACTION_RECEIVE_REWARD) {
            if ((params[1] as String).isEmpty()) {
                tipsMessage.postValueSupport(getString(R.string.hint_input_verification_code))
                return false
            }

            val inviterAddress = params[2] as String
            if (inviterAddress.isNotEmpty() && !validationViolasAddress(inviterAddress)) {
                tipsMessage.postValueSupport(getString(R.string.hint_inviter_address_error))
                return false
            }
        }

        return true
    }
}
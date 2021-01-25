package com.violas.wallet.ui.incentive.receiveRewards

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
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 11/25/20 4:55 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ReceiveIncentiveRewardsViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_VERIFICATION_CODE = 0
        const val ACTION_RECEIVE_REWARD = 1
    }

    private var violasAccount: AccountDO? = null
    val countryAreaLiveData = MutableLiveData<CountryAreaVO>()

    private val basicService by lazy {
        DataRepository.getBasicService()
    }
    private val incentiveService by lazy {
        DataRepository.getIncentiveService()
    }

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
        val walletAddress = violasAccount!!.address
        val phoneNumber = params[0] as String
        val areaCode = countryAreaLiveData.value!!.areaCode

        // 获取验证码操作
        if (action == ACTION_GET_VERIFICATION_CODE) {
            basicService.sendPhoneVerifyCode(
                walletAddress,
                phoneNumber,
                areaCode
            )
            tipsMessage.postValueSupport(getString(R.string.incentive_receive_tips_get_verification_code_success))
            return
        }

        // 领取激励奖励
        val verificationCode = params[1] as String
        val inviterAddress = params[2] as String
        incentiveService.receiveIncentiveRewards(
            walletAddress,
            phoneNumber,
            areaCode,
            verificationCode,
            inviterAddress
        )
        tipsMessage.postValueSupport(getString(R.string.incentive_receive_tips_receive_success))
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) return false

        val countryArea = countryAreaLiveData.value ?: return false
        if (violasAccount == null) {
            tipsMessage.postValueSupport(getString(R.string.common_tips_account_error))
            return false
        }

        val phoneNumber = params[0] as String
        if (phoneNumber.isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.incentive_receive_hint_phone_number))
            return false
        }

        if (action == ACTION_RECEIVE_REWARD) {
            if ((params[1] as String).isEmpty()) {
                tipsMessage.postValueSupport(getString(R.string.incentive_receive_hint_verification_code))
                return false
            }

            val inviterAddress = params[2] as String
            if (inviterAddress.isNotEmpty() && !validationViolasAddress(inviterAddress)) {
                tipsMessage.postValueSupport(getString(R.string.incentive_receive_tips_inviter_address_error))
                return false
            }
        }

        return true
    }
}
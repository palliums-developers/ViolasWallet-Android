package com.violas.wallet.ui.authentication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.utils.validationIDCar18
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2019-11-28 15:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class IDAuthenticationViewModel : BaseViewModel() {

    val countryAreaVO = MutableLiveData<CountryAreaVO>()
    val idCardFrontImage = MutableLiveData<Any?>()
    val idCardBackImage = MutableLiveData<Any?>()
    val authenticationResult = MutableLiveData<Boolean>()

    fun loadDefaultCountryArea() {
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

        tipsMessage.postValue(getString(R.string.hint_id_authentication_success))
        authenticationResult.postValue(true)
        onSuccess.invoke()
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) {
            return false
        }

        val countryAreaVO = countryAreaVO.value
        if (countryAreaVO == null) {
            tipsMessage.postValue(getString(R.string.hint_select_country_area))
            return false
        }

        val name = params[0] as String
        if (name.isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_name))
            return false
        }

        val idNumber = params[1] as String
        if (idNumber.isEmpty()) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_enter_id_number_china
                    } else {
                        R.string.hint_enter_id_number_other
                    }
                )
            )
            return false
        } else if (isChinaMainland(countryAreaVO) && !validationIDCar18(idNumber)) {
            tipsMessage.postValue(getString(R.string.hint_id_number_format_incorrect_china))
            return false
        }

        if (idCardFrontImage.value == null) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_photograph_id_card_front_china
                    } else {
                        R.string.hint_photograph_id_card_front_other
                    }
                )
            )
            return false
        } else if (idCardBackImage.value == null) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_photograph_id_card_back_china
                    } else {
                        R.string.hint_photograph_id_card_back_other
                    }
                )
            )
            return false
        }

        return true
    }
}
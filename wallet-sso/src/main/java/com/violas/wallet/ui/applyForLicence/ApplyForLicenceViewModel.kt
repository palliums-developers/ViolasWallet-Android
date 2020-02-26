package com.violas.wallet.ui.applyForLicence

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/2/26 16:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ApplyForLicenceViewModel : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()
    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val walletAddress = mAccountLD.value!!.address
        val name = mAccountLD.value!!.walletNickname
        val txid = params[0] as String
        mGovernorService.signUpGovernor(walletAddress, name, txid)
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty() || mAccountLD.value == null) {
            return false
        }

        val txid = params[0] as String
        if (txid.isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.hint_input_txid))
            return false
        }

        return true
    }
}
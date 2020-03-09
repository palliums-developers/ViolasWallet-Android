package com.violas.wallet.ui.signUpGovernor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.GovernorManager
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/2/26 16:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SignUpGovernorViewModel : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()
    private val mGovernorManager by lazy { GovernorManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        mGovernorManager.signUpGovernor(mAccountLD.value!!, params[0] as String)
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
package com.violas.wallet.ui.desktopManagement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/16 16:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LoginDesktopViewModelFactory(
    private val mSessionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(String::class.java)
            .newInstance(mSessionId)
    }
}

class LoginDesktopViewModel(
    private val mSessionId: String
) : BaseViewModel() {

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
        val account = params[0] as Account
        val signedSessionId = account.keyPair.sign(mSessionId.toByteArray()).toHex()

        mGovernorService.loginDesktop(
            walletAddress = mAccountLD.value!!.address,
            type = 1,
            signedSessionId = signedSessionId
        )
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty() || mAccountLD.value == null) {
            return false
        }

        return true
    }
}
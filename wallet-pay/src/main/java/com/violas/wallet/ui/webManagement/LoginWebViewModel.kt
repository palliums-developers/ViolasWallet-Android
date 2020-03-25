package com.violas.wallet.ui.webManagement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.webManagement.LoginWebActivity.Companion.SCAN_LOGIN_TYPE_WEB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/3/23 14:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LoginWebViewModelFactory(
    private val mSessionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(String::class.java)
            .newInstance(mSessionId)
    }
}

class LoginWebViewModel(
    private val mSessionId: String
) : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mViolasService by lazy { DataRepository.getViolasService() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val accounts = mAccountStorage.loadAll()
        mViolasService.loginWeb(SCAN_LOGIN_TYPE_WEB, mSessionId, accounts)
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        return mAccountLD.value != null
    }
}
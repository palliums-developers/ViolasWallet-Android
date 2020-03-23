package com.violas.wallet.ui.webManagement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.palliums.violascore.wallet.Account

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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val account = params[0] as Account

        // TODO 登录网页端逻辑
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty() || mAccountLD.value == null) {
            return false
        }

        return true
    }
}
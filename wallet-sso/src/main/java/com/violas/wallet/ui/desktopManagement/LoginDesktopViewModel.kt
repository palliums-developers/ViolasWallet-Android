package com.violas.wallet.ui.desktopManagement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.database.entity.AccountDO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/3/16 16:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LoginDesktopViewModel : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()
    val mLoginPwdErrorLD = MutableLiveData<Boolean>(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // TODO 登录逻辑
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty() || mAccountLD.value == null) {
            return false
        }

        val pwd = params[0] as String
        if (pwd.isEmpty()) {
            tipsMessage.postValueSupport(getString(R.string.hint_input_login_pwd))
            return false
        }

        return true
    }
}
package com.violas.wallet.ui.governorApproval

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.GovernorManager
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.ui.main.message.SSOApplicationMsgVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ApprovalActivityViewModelFactory(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationMsgVO::class.java)
            .newInstance(mSSOApplicationMsgVO)
    }
}

class ApprovalActivityViewModel(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()

    private val mGovernorManager by lazy { GovernorManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)

            // 进入消息详情页面自动标记本地消息为已读
            mGovernorManager.markSSOApplicationMsgAsRead(currentAccount.id, mSSOApplicationMsgVO)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // 加载SSO申请详情
        val ssoApplicationDetails =
            mGovernorManager.getSSOApplicationDetails(mAccountLD.value!!, mSSOApplicationMsgVO)
        mSSOApplicationDetailsLD.postValue(ssoApplicationDetails)
    }

    override fun isLoadAction(action: Int): Boolean {
        return true
    }
}
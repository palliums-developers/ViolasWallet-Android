package com.violas.wallet.ui.ssoApplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.SSOManager
import com.violas.wallet.event.SSOApplicationChangeEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SSOApplicationParentViewModelFactory(
    private val mSSOApplicationMsg: ApplyForStatusDTO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationParentViewModel::class.java)
            .newInstance(mSSOApplicationMsg)
    }
}

class SSOApplicationParentViewModel(
    private val mSSOApplicationMsg: ApplyForStatusDTO
) : BaseViewModel() {

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()

    private val mSSOManager by lazy { SSOManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // 加载SSO申请详情
        val details =
            mSSOManager.getSSOApplicationDetails(mAccountLD.value!!.address)

        if (details == null) {
            EventBus.getDefault().post(SSOApplicationChangeEvent(null))
        } else if (applicationChanged(details)) {
            EventBus.getDefault().post(
                SSOApplicationChangeEvent(
                    ApplyForStatusDTO.newInstance(details)
                )
            )
        }

        mSSOApplicationDetailsLD.postValue(details)
    }

    private fun applicationChanged(details: SSOApplicationDetailsDTO): Boolean {
        return mSSOApplicationMsg.applicationId != details.applicationId ||
                mSSOApplicationMsg.approvalStatus != details.applicationStatus ||
                mSSOApplicationMsg.tokenName != details.tokenName ||
                mSSOApplicationMsg.applicationDate != details.applicationDate ||
                mSSOApplicationMsg.expirationDate != details.expirationDate
    }
}
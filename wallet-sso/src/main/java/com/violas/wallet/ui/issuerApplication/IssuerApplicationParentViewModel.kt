package com.violas.wallet.ui.issuerApplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.event.SSOApplicationChangeEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.issuer.ApplyForSSODetailsDTO
import com.violas.wallet.repository.http.issuer.ApplyForSSOSummaryDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class IssuerApplicationParentViewModelFactory(
    private val mApplyForSSOSummary: ApplyForSSOSummaryDTO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(ApplyForSSOSummaryDTO::class.java)
            .newInstance(mApplyForSSOSummary)
    }
}

class IssuerApplicationParentViewModel(
    private val mApplyForSSOSummary: ApplyForSSOSummaryDTO
) : BaseViewModel() {

    val mAccountDOLiveData = MutableLiveData<AccountDO>()
    val mApplyForSSODetailsLiveData = MutableLiveData<ApplyForSSODetailsDTO?>()

    private val mIssuerService by lazy { DataRepository.getIssuerService() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountDOLiveData.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val details =
            if (mApplyForSSOSummary.applicationStatus != SSOApplicationState.IDLE)
                mIssuerService.getApplyForSSODetails(
                    mAccountDOLiveData.value!!.address
                )
            else
                null

        if (details == null && mApplyForSSOSummary.applicationStatus != SSOApplicationState.IDLE) {
            EventBus.getDefault().post(SSOApplicationChangeEvent(null))
        } else if (details != null && applicationChanged(details)) {
            EventBus.getDefault().post(
                SSOApplicationChangeEvent(
                    ApplyForSSOSummaryDTO.newInstance(details)
                )
            )
        }

        mApplyForSSODetailsLiveData.postValue(details)
    }

    private fun applicationChanged(details: ApplyForSSODetailsDTO): Boolean {
        return mApplyForSSOSummary.applicationStatus != details.applicationStatus ||
                mApplyForSSOSummary.applicationId != details.applicationId ||
                mApplyForSSOSummary.tokenName != details.tokenName ||
                mApplyForSSOSummary.tokenIdx != details.tokenIdx
    }
}
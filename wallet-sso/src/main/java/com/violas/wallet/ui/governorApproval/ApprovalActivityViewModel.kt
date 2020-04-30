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
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
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

    companion object {
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01        // 加载SSO申请详情
        const val ACTION_APPROVAL_APPLICATION = 0x10
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()
    val mUnapproveReasonsLD by lazy {
        MutableLiveData<List<UnapproveReasonDTO>>()
    }
    val mIsTransferredCoinToSSOLD by lazy {
        MutableLiveData<Boolean>()
    }

    private val mGovernorManager by lazy { GovernorManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)

            // 进入州长铸币页面自动标记本地消息为已读
            mGovernorManager.markSSOApplicationMsgAsRead(currentAccount.id, mSSOApplicationMsgVO)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_LOAD_APPLICATION_DETAILS -> {
                // 加载SSO申请详情
                val ssoApplicationDetails =
                    mGovernorManager.getSSOApplicationDetails(
                        mAccountLD.value!!,
                        mSSOApplicationMsgVO
                    )
                mSSOApplicationDetailsLD.postValue(ssoApplicationDetails)
            }

            ACTION_APPROVAL_APPLICATION -> {
                // TODO 删除
                // 审批操作成功后更新本地消息状态
                //mSSOApplicationMsgVO.applicationStatus = 1
                mSSOApplicationMsgVO.applicationStatus = 2
                mGovernorManager.updateSSOApplicationMsgStatus(
                    mAccountLD.value!!.id,
                    mSSOApplicationMsgVO
                )
            }

            else -> {
                throw IllegalArgumentException("Unrecognized action")
            }
        }
    }
}
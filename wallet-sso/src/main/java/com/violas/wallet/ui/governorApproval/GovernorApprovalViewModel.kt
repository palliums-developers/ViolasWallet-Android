package com.violas.wallet.ui.governorApproval

import android.content.Context
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
import com.violas.wallet.ui.transfer.TransferActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class GovernorApprovalViewModelFactory(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationMsgVO::class.java)
            .newInstance(mSSOApplicationMsgVO)
    }
}

class GovernorApprovalViewModel(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01
        const val ACTION_APPROVAL_APPLICATION = 0x02
        const val ACTION_NOTIFY_SSO = 0x03
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()

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
        if (action == ACTION_LOAD_APPLICATION_DETAILS) {
            // 加载申请详情
            val ssoApplicationDetails =
                mGovernorManager.getSSOApplicationDetails(mAccountLD.value!!, mSSOApplicationMsgVO)
            mSSOApplicationDetailsLD.postValue(ssoApplicationDetails)
            return
        } else if (action == ACTION_NOTIFY_SSO) {
            // 通知SSO发行商
            mGovernorManager.notifySSOCanApplyForMint(
                ssoApplicationDetails = mSSOApplicationDetailsLD.value!!,
                account = params[1] as Account
            )

            // TODO 跟新本地消息状态
            return
        }

        // 审批申请
        val passAndApply = params[0] as Boolean
        if (passAndApply) {
            // 审核通过，并申请铸币权
            mGovernorManager.passSSOApplicationAndApplyForMintPower(
                ssoApplicationDetails = mSSOApplicationDetailsLD.value!!,
                account = params[1] as Account
            )
        } else {
            // 审批不通过
            mGovernorManager.rejectSSOApplication(mSSOApplicationDetailsLD.value!!)
        }

        // 审批操作成功后更新本地消息状态
        mSSOApplicationMsgVO.applicationStatus = if (passAndApply) 1 else 2
        mGovernorManager.updateSSOApplicationMsgStatus(mAccountLD.value!!.id, mSSOApplicationMsgVO)
    }

    fun transferVTokenToSSO(context: Context, requestCode: Int) {
        val accountDO = mAccountLD.value
        val ssoApplicationDetailsDTO = mSSOApplicationDetailsLD.value
        if (accountDO == null || ssoApplicationDetailsDTO == null) {
            return
        }

        TransferActivity.start(
            context = context,
            accountId = accountDO.id,
            address = ssoApplicationDetailsDTO.ssoWalletAddress,
            amount = 100 * 1000_000,
            modifyable = false,
            requestCode = requestCode
        )
    }
}
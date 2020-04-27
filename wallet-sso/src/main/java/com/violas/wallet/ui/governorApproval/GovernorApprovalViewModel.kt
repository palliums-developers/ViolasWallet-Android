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
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
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
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01        // 加载SSO申请详情
        const val ACTION_LOAD_UNAPPROVE_REASONS = 0x02          // 加载审核不通过SSO申请原因列表
        const val ACTION_UNAPPROVE_APPLICATION = 0x03           // 审核不通过SSO申请
        const val ACTION_APPLY_FOR_MINT_POWER = 0x04            // 申请铸币权
        const val ACTION_APPROVAL_APPLICATION = 0x02
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    val mSSOApplicationDetailsLD = MutableLiveData<SSOApplicationDetailsDTO?>()
    val mUnapproveReasons = MutableLiveData<List<UnapproveReasonDTO>>()

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

            ACTION_LOAD_UNAPPROVE_REASONS -> {
                // 加载审核不通过SSO申请原因列表
                val unapproveReasons =
                    mGovernorManager.getUnapproveReasons()
                mUnapproveReasons.postValue(unapproveReasons)
            }

            ACTION_UNAPPROVE_APPLICATION -> {
                // 审核不通过SSO申请
                mGovernorManager.unapproveSSOApplication(
                    mSSOApplicationDetailsLD.value!!,
                    params[0] as Int,
                    params[1] as String
                )

                // 操作成功后更新本地消息状态
                mSSOApplicationMsgVO.applicationStatus = 2
                mGovernorManager.updateSSOApplicationMsgStatus(
                    mAccountLD.value!!.id,
                    mSSOApplicationMsgVO
                )
            }

            ACTION_APPLY_FOR_MINT_POWER -> {
                // 申请铸币权
                mGovernorManager.applyForMintPower(
                    mSSOApplicationDetailsLD.value!!,
                    account = params[0] as Account
                )

                // 审批操作成功后更新本地消息状态
                mSSOApplicationMsgVO.applicationStatus = 1
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
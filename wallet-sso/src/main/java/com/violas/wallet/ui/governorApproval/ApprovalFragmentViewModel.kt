package com.violas.wallet.ui.governorApproval

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.GovernorManager
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import com.violas.wallet.repository.http.governor.UnapproveReasonDTO
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
class ApprovalFragmentViewModelFactory(
    private val mSSOApplicationDetailsLD: SSOApplicationDetailsDTO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationDetailsDTO::class.java)
            .newInstance(mSSOApplicationDetailsLD)
    }
}

class ApprovalFragmentViewModel(
    private val mSSOApplicationDetails: SSOApplicationDetailsDTO
) : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_UNAPPROVE_REASONS = 0x01          // 加载审核不通过发币申请的原因列表
        const val ACTION_UNAPPROVE_APPLICATION = 0x02           // 审核不通过发币申请
        const val ACTION_APPLY_FOR_MINTABLE = 0x03              // 申请铸币权
        const val ACTION_APPROVE_APPLICATION = 0x04             // 审核通过发币申请
        const val ACTION_MINT_TOKEN_TO_SSO = 0x05               // 铸币给发行商
    }

    val mUnapproveReasonsLD by lazy {
        MutableLiveData<List<UnapproveReasonDTO>>()
    }
    val mIsTransferredCoinToSSOLD by lazy {
        MutableLiveData<Boolean>()
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    private val mGovernorManager by lazy { GovernorManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)

            if (mSSOApplicationDetails.applicationStatus == SSOApplicationState.CHAIRMAN_APPROVED) {
                val transferred = mGovernorManager.isTransferredCoinToSSO(
                    walletAddress = currentAccount.address,
                    applicationId = mSSOApplicationDetails.applicationId
                )
                mIsTransferredCoinToSSOLD.postValue(transferred)
            }
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_LOAD_UNAPPROVE_REASONS -> {
                // 加载审核不通过SSO申请原因列表
                val unapproveReasons =
                    mGovernorManager.getUnapproveReasons()
                mUnapproveReasonsLD.postValue(unapproveReasons)
            }

            ACTION_UNAPPROVE_APPLICATION -> {
                // 审核不通过SSO申请
                mGovernorManager.unapproveSSOApplication(
                    mSSOApplicationDetails,
                    params[0] as Int,
                    params[1] as String
                )
            }

            ACTION_APPLY_FOR_MINTABLE -> {
                // 申请铸币权
                mGovernorManager.applyForMintable(
                    mSSOApplicationDetails,
                    mAccountLD.value!!.address
                )
            }

            ACTION_APPROVE_APPLICATION -> {
                // 已向发行商转VToken，通知发行商可以申请铸币
                mGovernorManager.approveSSOApplication(
                    mSSOApplicationDetails,
                    null,
                    mAccountLD.value!!.address
                )
            }

            ACTION_MINT_TOKEN_TO_SSO -> {
                // 铸币给发行商
                mGovernorManager.mintTokenToSSOAccount(
                    ssoApplicationDetails = mSSOApplicationDetails,
                    account = params[0] as Account
                )
            }

            else -> {
                throw IllegalArgumentException("Unrecognized action")
            }
        }
    }

    fun transferCoinToSSO(fragment: Fragment, requestCode: Int) {
        val accountDO = mAccountLD.value ?: return

        TransferActivity.start(
            fragment = fragment,
            accountId = accountDO.id,
            address = mSSOApplicationDetails.ssoWalletAddress,
            amount = 2 * 1000_000,
            modifyable = false,
            requestCode = requestCode
        )
    }
}
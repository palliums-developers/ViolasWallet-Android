package com.violas.wallet.ui.governorMint

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
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class GovernorMintViewModelFactory(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationMsgVO::class.java)
            .newInstance(mSSOApplicationMsgVO)
    }
}

class GovernorMintViewModel(
    private val mSSOApplicationMsgVO: SSOApplicationMsgVO
) : BaseViewModel() {

    companion object {
        const val ACTION_LOAD_APPLICATION_DETAILS = 0x01
        const val ACTION_MINT_TOKEN_TO_SSO_ACCOUNT = 0x02
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
        }

        // 给SSO账户铸币
        mGovernorManager.mintTokenToSSOAccount(
            ssoApplicationDetails = mSSOApplicationDetailsLD.value!!,
            account = params[0] as Account,
            mnemonics = params[1] as List<String>
        )

        // 铸币成功后更新本地消息状态
        mSSOApplicationMsgVO.applicationStatus = 4
        mGovernorManager.updateSSOApplicationMsgStatus(mAccountLD.value!!.id, mSSOApplicationMsgVO)
    }
}
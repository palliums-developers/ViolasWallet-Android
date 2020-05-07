package com.violas.wallet.ui.ssoApplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.SSOManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/3/4 15:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SSOApplicationChildViewModelFactory(
    private val mSSOApplicationDetails: SSOApplicationDetailsDTO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass
            .getConstructor(SSOApplicationDetailsDTO::class.java)
            .newInstance(mSSOApplicationDetails)
    }
}

class SSOApplicationChildViewModel(
    private val mSSOApplicationDetails: SSOApplicationDetailsDTO
) : BaseViewModel() {

    companion object {
        const val ACTION_APPLY_FOR_ISSUE_TOKEN = 0x01           // 申请发币
        const val ACTION_APPLY_FOR_MINT_TOKEN = 0x02            // 申请铸币
    }

    val mAccountLD = MutableLiveData<AccountDO>()
    private val mSSOManager by lazy { SSOManager() }
    private val mTokenManager by lazy { TokenManager() }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_APPLY_FOR_ISSUE_TOKEN -> {
                //TODO 申请发币
            }

            ACTION_APPLY_FOR_MINT_TOKEN -> {
                // 申请铸币
                val account = params[0] as Account
                val walletAddress = account.getAddress().toHex()

                // 1.检测合约是否publish，没有则先publish
                val published = mTokenManager.isPublishedContract(walletAddress)
                if (!published) {
                    mTokenManager.publishContract(account)

                    // 本地记录新发行的token
                    val assertToken = AssertToken(
                        account_id = mAccountLD.value!!.id,
                        fullName = mSSOApplicationDetails.tokenName,
                        name = mSSOApplicationDetails.tokenName,
                        tokenIdx = mSSOApplicationDetails.tokenIdx!!,
                        enable = true
                    )
                    mTokenManager.insert(true, assertToken)
                }

                // 2.通知服务器发行商已publish合约
                mSSOManager.changePublishStatus(walletAddress)

                // 将新发行的token显示在钱包首页
                EventBus.getDefault().post(SwitchAccountEvent())
            }

            else -> {
                throw IllegalArgumentException("Unrecognized action")
            }
        }
    }
}
package com.violas.wallet.ui.main.applyFor

import android.util.Log
import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.AuthenticationCompleteEvent
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.SSOApplicationChangeEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ApplyForSSOViewModel : BaseViewModel() {

    companion object {
        const val CODE_AUTHENTICATION_ACCOUNT = Int.MIN_VALUE
        const val CODE_AUTHENTICATION_COMPLETE = CODE_AUTHENTICATION_ACCOUNT + 1
    }

    private val mSSOService by lazy { DataRepository.getSSOService() }
    private val mTokenManager by lazy { TokenManager() }

    val mAccountDOLiveData = MutableLiveData<AccountDO>()
    val mIssueSSOStatusLiveData = EnhancedMutableLiveData<ApplyForStatusDTO>()

    init {
        EventBus.getDefault().register(this)
        viewModelScope.launch(Dispatchers.IO) {
            val accountDO = AccountManager().currentAccount()
            mAccountDOLiveData.postValue(accountDO)
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    @Subscribe
    fun onSSOApplicationChangeEvent(event: SSOApplicationChangeEvent) {
        val issueSSOStatus =
            event.status ?: ApplyForStatusDTO(CODE_AUTHENTICATION_COMPLETE)
        mIssueSSOStatusLiveData.postValueSupport(issueSSOStatus)
    }

    @Subscribe
    fun onAuthenticationCompleteEvent(event: AuthenticationCompleteEvent) {
        mIssueSSOStatusLiveData.postValueSupport(ApplyForStatusDTO(CODE_AUTHENTICATION_COMPLETE))
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val issueSSOStatus = coroutineScope {
            val userInfoDeferred =
                async {
                    mSSOService.loadUserInfo(mAccountDOLiveData.value!!.address)?.data
                }

            val remoteIssueSSOStatusDeferred =
                async {
                    mSSOService.selectApplyForStatus(mAccountDOLiveData.value!!.address)?.data
                }

            val userInfo = userInfoDeferred.await()
            val remoteIssueSSOStatus = remoteIssueSSOStatusDeferred.await()

            if (userInfo == null
                || userInfo.countryCode.isNullOrEmpty()
                || userInfo.idName.isNullOrEmpty()
                || userInfo.idNumber.isNullOrEmpty()
                || userInfo.idPhotoFrontUrl.isNullOrEmpty()
                || userInfo.idPhotoBackUrl.isNullOrEmpty()
                || userInfo.emailAddress.isNullOrEmpty()
                || userInfo.phoneNumber.isNullOrEmpty()
                || userInfo.phoneAreaCode.isNullOrEmpty()
            ) {
                return@coroutineScope ApplyForStatusDTO(CODE_AUTHENTICATION_ACCOUNT)
            }

            if (remoteIssueSSOStatus == null) {
                return@coroutineScope ApplyForStatusDTO(CODE_AUTHENTICATION_COMPLETE)
            }

            if (remoteIssueSSOStatus.approvalStatus == SSOApplicationState.MINTED_TOKEN) {
                refreshAssert(mAccountDOLiveData.value!!, remoteIssueSSOStatus)
            }


            Log.e("TEST", "coroutineScope =>  end")
            return@coroutineScope remoteIssueSSOStatus!!
        }

        mIssueSSOStatusLiveData.postValueSupport(issueSSOStatus)
        Log.e("TEST", "realExecute =>  end")
    }

    private fun refreshAssert(account: AccountDO, applyToken: ApplyForStatusDTO) {
        if (mTokenManager.findTokenByTokenAddress(account.id, applyToken.tokenIdx) != null) {
            return
        }

        mTokenManager.insert(
            true,
            AssertToken(
                account_id = account.id,
                tokenIdx = applyToken.tokenIdx,
                name = applyToken.tokenName,
                fullName = applyToken.tokenName,
                enable = true
            )
        )
        EventBus.getDefault().post(RefreshBalanceEvent())
    }
}
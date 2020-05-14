package com.violas.wallet.ui.main.applyFor

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
import com.violas.wallet.repository.http.issuer.ApplyForSSOSummaryDTO
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

    private val mIssuerService by lazy { DataRepository.getIssuerService() }
    private val mTokenManager by lazy { TokenManager() }

    val mAccountDOLiveData = MutableLiveData<AccountDO>()
    val mApplyForSSOSummaryLiveData = EnhancedMutableLiveData<ApplyForSSOSummaryDTO>()

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
        mApplyForSSOSummaryLiveData.postValueSupport(
            event.summary ?: ApplyForSSOSummaryDTO(CODE_AUTHENTICATION_COMPLETE)
        )
    }

    @Subscribe
    fun onAuthenticationCompleteEvent(event: AuthenticationCompleteEvent) {
        mApplyForSSOSummaryLiveData.postValueSupport(
            ApplyForSSOSummaryDTO(CODE_AUTHENTICATION_COMPLETE)
        )
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val summary = coroutineScope {
            val userInfoDeferred =
                async {
                    mIssuerService.loadUserInfo(mAccountDOLiveData.value!!.address).data
                }

            val remoteSummaryDeferred =
                async {
                    mIssuerService.queryApplyForSSOSummary(mAccountDOLiveData.value!!.address)
                }

            val userInfo = userInfoDeferred.await()
            val remoteSummary = remoteSummaryDeferred.await()

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
                return@coroutineScope ApplyForSSOSummaryDTO(CODE_AUTHENTICATION_ACCOUNT)
            }

            if (remoteSummary == null) {
                return@coroutineScope ApplyForSSOSummaryDTO(CODE_AUTHENTICATION_COMPLETE)
            }

            if (remoteSummary.applicationStatus == SSOApplicationState.GOVERNOR_MINTED) {
                refreshAssert(mAccountDOLiveData.value!!, remoteSummary)
            }

            return@coroutineScope remoteSummary!!
        }

        mApplyForSSOSummaryLiveData.postValueSupport(summary)
    }

    private fun refreshAssert(account: AccountDO, summary: ApplyForSSOSummaryDTO) {
        if (mTokenManager.findTokenByTokenAddress(account.id, summary.tokenIdx!!) != null) {
            return
        }

        mTokenManager.insert(
            true,
            AssertToken(
                account_id = account.id,
                tokenIdx = summary.tokenIdx,
                name = summary.tokenName,
                fullName = summary.tokenName,
                enable = true
            )
        )
        EventBus.getDefault().post(RefreshBalanceEvent())
    }

    override fun isLoadAction(action: Int): Boolean {
        return true
    }
}
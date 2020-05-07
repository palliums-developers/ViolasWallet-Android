package com.violas.wallet.ui.main.applyFor

import androidx.lifecycle.*
import com.palliums.net.LoadState
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.biz.SSOManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import com.violas.wallet.ui.main.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ApplyForSSOViewModelFactory(
    private val userViewModel: UserViewModel
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ApplyForSSOViewModel(userViewModel) as T
    }
}

class ApplyForSSOViewModel(
    private val userViewModel: UserViewModel
) : ViewModel() {

    companion object {
        const val CODE_NETWORK_ERROR = Int.MIN_VALUE + 1
        const val CODE_VERIFICATION_ACCOUNT = CODE_NETWORK_ERROR + 1
        const val CODE_VERIFICATION_SUCCESS = CODE_VERIFICATION_ACCOUNT + 1
        const val CODE_NETWORK_LOADING = CODE_VERIFICATION_SUCCESS + 1
    }

    private val mApplyManager by lazy {
        SSOManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    private var mAccount: AccountDO? = null
    // 0 1 2 3 4 业务状态
    private val mApplyStatus = MediatorLiveData<ApplyForStatusDTO>()
    private val mNetWorkApplyStatus = EnhancedMutableLiveData<ApplyForStatusDTO>()
    private val mApplyStatusLoadState = EnhancedMutableLiveData<LoadState>()
    private val mLock: Any = Any()

    init {
        EventBus.getDefault().register(this)
        mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_LOADING)

        // 监听SSO发币申请的状态
        mApplyStatus.addSource(mNetWorkApplyStatus) {
            synchronized(mLock) {
                val userInfoAllReady =
                    userViewModel.getAllReadyLiveData().value?.peekData()
                if (userInfoAllReady != null) {
                    if (userInfoAllReady) {
                        mApplyStatus.value = it.peekData()
                    } else {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_VERIFICATION_ACCOUNT)
                    }
                    return@addSource
                }

                val userInfoLoadState =
                    userViewModel.loadState.value?.getDataIfNotHandled()
                if (userInfoLoadState != null) {
                    if (userInfoLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    }
                }
            }
        }

        // 监听查询SSO发币申请信息的进度
        mApplyStatus.addSource(mApplyStatusLoadState) {
            synchronized(mLock) {
                if (it.peekData().status != LoadState.Status.FAILURE) {
                    return@addSource
                }

                val userInfoAllReady =
                    userViewModel.getAllReadyLiveData().value?.peekData()
                if (userInfoAllReady != null) {
                    mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    return@addSource
                }

                val userInfoLoadState =
                    userViewModel.loadState.value?.getDataIfNotHandled()
                if (userInfoLoadState != null) {
                    if (userInfoLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    }
                }
            }
        }

        // 监听用户信息allReady的状态
        mApplyStatus.addSource(userViewModel.getAllReadyLiveData()) {
            synchronized(mLock) {
                val netWorkApplyStatus =
                    mNetWorkApplyStatus.value?.peekData()
                if (netWorkApplyStatus != null) {
                    if (it.peekData()) {
                        mApplyStatus.value = netWorkApplyStatus
                    } else {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_VERIFICATION_ACCOUNT)
                    }
                    return@addSource
                }

                val applyStatusLoadState =
                    mApplyStatusLoadState.value?.getDataIfNotHandled()
                if (applyStatusLoadState != null) {
                    if (applyStatusLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    }
                }
            }
        }

        // 监听查询用户信息的进度
        mApplyStatus.addSource(userViewModel.loadState) {
            synchronized(mLock) {
                if (it.peekData().status != LoadState.Status.FAILURE) {
                    return@addSource
                }

                val netWorkApplyStatus =
                    mNetWorkApplyStatus.value?.peekData()
                if (netWorkApplyStatus != null) {
                    mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    return@addSource
                }

                val applyStatusLoadState =
                    mApplyStatusLoadState.value?.getDataIfNotHandled()
                if (applyStatusLoadState != null) {
                    if (applyStatusLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = ApplyForStatusDTO(CODE_NETWORK_ERROR)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()

            refreshApplyStatusByNetwork(mAccount!!)
        }
    }

    fun getApplyStatusLiveData(): LiveData<ApplyForStatusDTO> = synchronized(mLock) { mApplyStatus }

    @Subscribe
    fun onRefreshPage(event: RefreshPageEvent? = null) {
        refreshApplyStatus()
    }

    fun refreshApplyStatus() {
        if (mAccount == null) {
            return
        }

        synchronized(mLock) {
            val applyStatue = mApplyStatus.value
            if (applyStatue == null || applyStatue.approvalStatus == CODE_NETWORK_LOADING) {
                return
            }

            mApplyStatus.postValue(ApplyForStatusDTO(CODE_NETWORK_LOADING))
        }

        viewModelScope.launch(Dispatchers.IO) {
            val loadState = userViewModel.loadState.value?.peekData()
            if (loadState != null
                && loadState.status == LoadState.Status.FAILURE
            ) {
                userViewModel.execute(checkNetworkBeforeExecute = false)
            }
            refreshApplyStatusByNetwork(mAccount!!)
        }
    }

    private suspend fun refreshApplyStatusByNetwork(account: AccountDO) {
        try {
            synchronized(mLock) {
                mApplyStatusLoadState.postValueSupport(LoadState.RUNNING)
            }

            val response =
                mApplyManager.getApplyStatus(account.address, false)

            synchronized(mLock) {
                if (response == null) {
                    mApplyStatusLoadState.postValueSupport(LoadState.failure(""))
                    return
                }

                if (response.errorCode == 2006 || response.data == null) {
                    mNetWorkApplyStatus.postValueSupport(ApplyForStatusDTO(CODE_VERIFICATION_SUCCESS))
                } else {
                    mNetWorkApplyStatus.postValueSupport(response.data!!)

                    if (response.data!!.approvalStatus == SSOApplicationState.MINTED_TOKEN) {
                        refreshAssert(account, response.data!!)
                    }
                }

                mApplyStatusLoadState.postValueSupport(LoadState.SUCCESS)
            }
        } catch (e: Exception) {
            synchronized(mLock) {
                mApplyStatusLoadState.postValueSupport(LoadState.failure(e))
            }
        }
    }

    private fun refreshAssert(account: AccountDO, applyToken: ApplyForStatusDTO) {
        applyToken.tokenIdx.let {
            if (mTokenManager.findTokenByTokenAddress(account.id, it) != null) {
                return
            }

            mTokenManager.insert(
                true,
                AssertToken(
                    account_id = account.id,
                    tokenIdx = it,
                    name = applyToken.tokenName,
                    fullName = applyToken.tokenName,
                    enable = true
                )
            )
            EventBus.getDefault().post(RefreshBalanceEvent())
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}
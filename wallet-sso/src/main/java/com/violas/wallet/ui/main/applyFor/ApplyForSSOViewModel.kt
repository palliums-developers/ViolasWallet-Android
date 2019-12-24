package com.violas.wallet.ui.main.applyFor

import androidx.lifecycle.*
import com.palliums.net.LoadState
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ApplyManager
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
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ApplyForSSOViewModel(userViewModel) as T
    }
}

class ApplyForSSOViewModel(private val userViewModel: UserViewModel) :
    ViewModel() {

    companion object {
        const val CODE_NETWORK_ERROR = -1
        const val CODE_VERIFICATION_ACCOUNT = -2
        const val CODE_APPLY_SSO = -3
        const val CODE_NETWORK_LOADING = -4
    }

    private val mApplyManager by lazy {
        ApplyManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    private var mAccount: AccountDO? = null
    // 0 1 2 3 4 业务状态
    private val mApplyStatus = MediatorLiveData<Int>()
    private val mNetWorkApplyStatus = MutableLiveData<Int>()
    private val mApplyStatusLoadState = MutableLiveData<LoadState>()
    private val mLock: Any = Any()

    init {
        EventBus.getDefault().register(this)
        mApplyStatus.value = CODE_NETWORK_LOADING

        // 监听SSO发币申请的状态
        mApplyStatus.addSource(mNetWorkApplyStatus) {
            synchronized(mLock) {
                val userInfoAllReady = userViewModel.getAllReadyLiveData().value
                if (userInfoAllReady != null) {
                    if (userInfoAllReady) {
                        mApplyStatus.value = it
                    } else {
                        mApplyStatus.value = CODE_VERIFICATION_ACCOUNT
                    }
                    return@addSource
                }

                val userInfoLoadState = userViewModel.loadState.value
                if (userInfoLoadState != null) {
                    if (userInfoLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = CODE_NETWORK_ERROR
                    }
                }
            }
        }

        // 监听查询SSO发币申请信息的进度
        mApplyStatus.addSource(mApplyStatusLoadState) {
            synchronized(mLock) {
                if (it.status != LoadState.Status.FAILURE) {
                    return@addSource
                }

                val userInfoAllReady = userViewModel.getAllReadyLiveData().value
                if (userInfoAllReady != null) {
                    mApplyStatus.value = CODE_NETWORK_ERROR
                    return@addSource
                }

                val userInfoLoadState = userViewModel.loadState.value
                if (userInfoLoadState != null) {
                    if (userInfoLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = CODE_NETWORK_ERROR
                    }
                }
            }
        }

        // 监听用户信息allReady的状态
        mApplyStatus.addSource(userViewModel.getAllReadyLiveData()) {
            synchronized(mLock) {
                val netWorkApplyStatus = mNetWorkApplyStatus.value
                if (netWorkApplyStatus != null) {
                    if (it) {
                        mApplyStatus.value = netWorkApplyStatus
                    } else {
                        mApplyStatus.value = CODE_VERIFICATION_ACCOUNT
                    }
                    return@addSource
                }

                val applyStatusLoadState = mApplyStatusLoadState.value
                if (applyStatusLoadState != null) {
                    if (applyStatusLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = CODE_NETWORK_ERROR
                    }
                }
            }
        }

        // 监听查询用户信息的进度
        mApplyStatus.addSource(userViewModel.loadState) {
            synchronized(mLock) {
                if (it.status != LoadState.Status.FAILURE) {
                    return@addSource
                }

                val netWorkApplyStatus = mNetWorkApplyStatus.value
                if (netWorkApplyStatus != null) {
                    mApplyStatus.value = CODE_NETWORK_ERROR
                    return@addSource
                }

                val applyStatusLoadState = mApplyStatusLoadState.value
                if (applyStatusLoadState != null) {
                    if (applyStatusLoadState.status == LoadState.Status.FAILURE) {
                        mApplyStatus.value = CODE_NETWORK_ERROR
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()
            userViewModel.init()
            refreshApplyStatusByNetwork(mAccount!!)
        }
    }

    fun getApplyStatusLiveData(): LiveData<Int> = synchronized(mLock) { mApplyStatus }

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
            if (applyStatue == null || applyStatue == CODE_NETWORK_LOADING) {
                return
            }

            mApplyStatus.postValue(CODE_NETWORK_LOADING)
        }

        viewModelScope.launch(Dispatchers.IO) {
            userViewModel.execute()
            refreshApplyStatusByNetwork(mAccount!!)
        }
    }

    private suspend fun refreshApplyStatusByNetwork(account: AccountDO) {
        try {
            synchronized(mLock) {
                mApplyStatusLoadState.postValue(LoadState.RUNNING)
            }

            val response =
                mApplyManager.getApplyStatus(account.address, false)

            synchronized(mLock) {
                if (response == null) {
                    mApplyStatusLoadState.postValue(LoadState.failure(""))
                    return
                }

                if (response.errorCode == 2006 || response.data == null) {
                    mNetWorkApplyStatus.postValue(CODE_APPLY_SSO)
                } else {
                    val approvalStatus = response.data!!.approval_status
                    mNetWorkApplyStatus.postValue(approvalStatus)

                    if (approvalStatus == 4) {
                        refreshAssert(account, response.data!!)
                    }
                }

                mApplyStatusLoadState.postValue(LoadState.SUCCESS)
            }
        } catch (e: Exception) {
            synchronized(mLock) {
                mApplyStatusLoadState.postValue(LoadState.failure(e))
            }
        }
    }

    private fun refreshAssert(account: AccountDO, applyToken: ApplyForStatusDTO) {
        applyToken.token_address?.let {
            if (mTokenManager.findTokenByTokenAddress(account.id, it) != null) {
                return
            }

            mTokenManager.insert(
                true,
                AssertToken(
                    account_id = account.id,
                    tokenAddress = it,
                    name = applyToken.token_name,
                    fullName = applyToken.token_name,
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
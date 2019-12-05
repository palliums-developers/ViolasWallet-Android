package com.violas.wallet.ui.main.applyFor

import androidx.lifecycle.*
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.ApplyManager
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.main.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var mAccount: AccountDO? = null
    // 0 1 2 3 4 业务状态
    private val mApplyStatus = MediatorLiveData<Int>()
    private val mNetWorkApplyStatus = MutableLiveData<Int>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mAccount = mAccountManager.currentAccount()
        }
        EventBus.getDefault().register(this)
        mApplyStatus.value = CODE_NETWORK_LOADING
        mApplyStatus.addSource(mNetWorkApplyStatus, Observer {
            mApplyStatus.value = it
        })
        mApplyStatus.addSource(userViewModel.getAllReadyLiveData(), Observer {
            if (it && mApplyStatus.value ?: CODE_NETWORK_ERROR < 0) {
                mApplyStatus.value = CODE_APPLY_SSO
            } else if (it) {
                refreshApplyStatus()
            } else {
                mApplyStatus.value = CODE_VERIFICATION_ACCOUNT
            }
        })
        userViewModel.init()
        refreshApplyStatus()
    }

    fun getApplyStatusLiveData() = mApplyStatus

    @Subscribe
    fun onRefreshPage(event: RefreshPageEvent? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshApplyStatusByNetwork()
        }
    }

    fun refreshApplyStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshApplyStatusByNetwork()
        }
    }

    private suspend fun refreshApplyStatusByNetwork() {
        mAccount?.let {
            mApplyStatus.postValue(CODE_NETWORK_LOADING)
            withContext(Dispatchers.IO) {
                val changePublishStatus = mApplyManager.getApplyStatus(it.address)
                if (changePublishStatus != null) {
                    when {
                        changePublishStatus.errorCode == 2006 -> when {
                            userViewModel.getAllReadyLiveData().value == null -> mApplyStatus.postValue(
                                CODE_NETWORK_ERROR
                            )
                            userViewModel.getAllReadyLiveData().value == true -> mApplyStatus.postValue(
                                CODE_APPLY_SSO
                            )
                            else -> mApplyStatus.postValue(CODE_VERIFICATION_ACCOUNT)
                        }
                        changePublishStatus.errorCode == 2000 -> mApplyStatus.postValue(
                            changePublishStatus.data?.approval_status
                        )
                        else -> mApplyStatus.postValue(CODE_NETWORK_ERROR)
                    }
                } else {
                    mApplyStatus.postValue(CODE_NETWORK_ERROR)
                }
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}
package com.violas.wallet.ui.main

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.palliums.net.LoadState
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.AuthenticationIDEvent
import com.violas.wallet.event.BindEmailEvent
import com.violas.wallet.event.BindPhoneEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.local.user.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by elephant on 2019-11-29 17:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun ComponentActivity.provideUserViewModel(): UserViewModel {
    return viewModels<UserViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return UserViewModel() as T
            }
        }
    }.value
}

class UserViewModel : BaseViewModel() {

    private lateinit var currentAccount: AccountDO

    private val ssoService by lazy {
        DataRepository.getSSOService()
    }

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    private val idInfo = MutableLiveData<IDInfo>()
    private val emailInfo = MutableLiveData<EmailInfo>()
    private val phoneInfo = MutableLiveData<PhoneInfo>()

    /**
     * 表示身份已认证，邮箱已绑定，手机已绑定
     */
    private val allReady = MutableLiveData<Boolean>()

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    fun getIdInfo(): LiveData<IDInfo> {
        synchronized(lock) {
            return this.idInfo
        }
    }

    fun getEmailInfo(): LiveData<EmailInfo> {
        synchronized(lock) {
            return this.emailInfo
        }
    }

    fun getPhoneInfo(): LiveData<PhoneInfo> {
        synchronized(lock) {
            return this.phoneInfo
        }
    }

    fun getAllReady(): LiveData<Boolean> {
        synchronized(lock) {
            return this.allReady
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onAuthenticationIDEvent(event: AuthenticationIDEvent) {
        synchronized(lock) {
            this.idInfo.postValue(event.idInfo)

            val emailInfo = this.emailInfo.value
            val phoneInfo = this.phoneInfo.value
            if (emailInfo != null && emailInfo.isBoundEmail()
                && phoneInfo != null && phoneInfo.isBoundPhone()
            ) {
                allReady.postValue(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onBindEmailEvent(event: BindEmailEvent) {
        synchronized(lock) {
            this.emailInfo.postValue(event.emailInfo)

            val idInfo = this.idInfo.value
            val phoneInfo = this.phoneInfo.value
            if (idInfo != null && idInfo.isAuthenticatedID()
                && phoneInfo != null && phoneInfo.isBoundPhone()
            ) {
                allReady.postValue(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onBindPhoneEvent(event: BindPhoneEvent) {
        synchronized(lock) {
            this.phoneInfo.postValue(event.phoneInfo)

            val idInfo = this.idInfo.value
            val emailInfo = this.emailInfo.value
            if (idInfo != null && idInfo.isAuthenticatedID()
                && emailInfo != null && emailInfo.isBoundEmail()
            ) {
                allReady.postValue(true)
            }
        }
    }

    fun init() {
        synchronized(lock) {
            if (this.idInfo.value != null) {
                return
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            synchronized(lock) {
                // 通知开始加载，此时外部获取用户信息为空
                loadState.postValue(LoadState.RUNNING)

                var allReady = true

                val idInfo = localUserService.getIDInfo()
                if (!idInfo.isAuthenticatedID()) {
                    // 身份认证状态不为已认证先当作未知状态
                    idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNKNOWN
                    allReady = false
                }
                this@UserViewModel.idInfo.postValue(idInfo)

                val emailInfo = localUserService.getEmailInfo()
                if (!emailInfo.isBoundEmail()) {
                    // 邮箱绑定状态不为已绑定先当作未知状态
                    emailInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
                    allReady = false
                }
                this@UserViewModel.emailInfo.postValue(emailInfo)

                val phoneInfo = localUserService.getPhoneInfo()
                if (!phoneInfo.isBoundPhone()) {
                    // 手机绑定状态不为已绑定先当作未知状态
                    phoneInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
                    allReady = false
                }
                this@UserViewModel.phoneInfo.postValue(phoneInfo)

                if (allReady) {
                    this@UserViewModel.allReady.postValue(true)
                    loadState.postValue(LoadState.SUCCESS)
                    return@launch
                }

                // 再次通知在加载中，用以展示loading，此时外部获取用户信息不为空
                loadState.postValue(LoadState.RUNNING)
            }

            // 为了loading的连贯性，此处没有调用execute，而是直接调用的realExecute
            // 注意execute内部处理了异常，这里需要处理异常情况
            try {
                currentAccount = AccountManager().currentAccount()

                realExecute(-1)

                synchronized(lock) {
                    loadState.postValue(LoadState.SUCCESS)
                }
            } catch (e: Exception) {
                synchronized(lock) {
                    tipsMessage.postValue(e.message)

                    loadState.postValue(LoadState.failure(e))
                }
            }
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {

        delay(2000)

        // 从服务器获取用户信息
        val walletAddress = currentAccount.address
        val userInfoDTO = ssoService.loadUserInfo(walletAddress).data

        synchronized(lock) {
            // 解析存储分发身份信息
            val idInfo = this.idInfo.value!!
            if (userInfoDTO == null
                || userInfoDTO.idName.isNullOrEmpty()
                || userInfoDTO.idNumber.isNullOrEmpty()
                || userInfoDTO.idPhotoFrontUrl.isNullOrEmpty()
                || userInfoDTO.idPhotoBackUrl.isNullOrEmpty()
                || userInfoDTO.countryCode.isNullOrEmpty()
            ) {
                idInfo.idName = ""
                idInfo.idNumber = ""
                idInfo.idPhotoFrontUrl = ""
                idInfo.idPhotoBackUrl = ""
                idInfo.idCountryCode = ""
                idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNAUTHORIZED
            } else {
                idInfo.idName = userInfoDTO.idName
                idInfo.idNumber = userInfoDTO.idNumber
                idInfo.idPhotoFrontUrl = userInfoDTO.idPhotoFrontUrl
                idInfo.idPhotoBackUrl = userInfoDTO.idPhotoBackUrl
                idInfo.idCountryCode = userInfoDTO.countryCode
                idInfo.idAuthenticationStatus = IDAuthenticationStatus.AUTHENTICATED
            }
            this.localUserService.setIDInfo(idInfo)
            this.idInfo.postValue(idInfo)

            // 解析存储分发邮箱信息
            val emailInfo = this.emailInfo.value!!
            if (userInfoDTO == null
                || userInfoDTO.emailAddress.isNullOrEmpty()
            ) {
                emailInfo.emailAddress = ""
                emailInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            } else {
                emailInfo.emailAddress = userInfoDTO.emailAddress
                emailInfo.accountBindingStatus = AccountBindingStatus.BOUND
            }
            this.localUserService.setEmailInfo(emailInfo)
            this.emailInfo.postValue(emailInfo)

            // 解析存储分发手机信息
            val phoneInfo = this.phoneInfo.value!!
            if (userInfoDTO == null
                || userInfoDTO.phoneAreaCode.isNullOrEmpty()
                || userInfoDTO.phoneNumber.isNullOrEmpty()
            ) {
                phoneInfo.areaCode = ""
                phoneInfo.phoneNumber = ""
                phoneInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            } else {
                phoneInfo.areaCode = if (userInfoDTO.phoneAreaCode.startsWith("+")) {
                    userInfoDTO.phoneAreaCode.substring(1)
                } else {
                    userInfoDTO.phoneAreaCode
                }
                phoneInfo.phoneNumber = userInfoDTO.phoneNumber
                phoneInfo.accountBindingStatus = AccountBindingStatus.BOUND
            }
            this.localUserService.setPhoneInfo(phoneInfo)
            this.phoneInfo.postValue(phoneInfo)

            // 分发申请发行准备进度
            this.allReady.postValue(
                idInfo.isAuthenticatedID()
                        && emailInfo.isBoundEmail()
                        && phoneInfo.isBoundPhone()
            )
        }
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        val idInfo = this.idInfo.value
        val emailInfo = this.emailInfo.value
        val phoneInfo = this.phoneInfo.value

        return (idInfo != null && !idInfo.isAuthenticatedID())
                || (emailInfo != null && !emailInfo.isBoundEmail())
                || (phoneInfo != null && !phoneInfo.isBoundPhone())
    }
}
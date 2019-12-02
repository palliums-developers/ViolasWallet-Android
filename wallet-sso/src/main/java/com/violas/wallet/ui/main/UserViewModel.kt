package com.violas.wallet.ui.main

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.*
import com.palliums.base.BaseViewModel
import com.violas.wallet.event.AuthenticationIDEvent
import com.violas.wallet.event.BindEmailEvent
import com.violas.wallet.event.BindPhoneEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.local.user.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

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
        return this.idInfo
    }

    fun getEmailInfo(): LiveData<EmailInfo> {
        return this.emailInfo
    }

    fun getPhoneInfo(): LiveData<PhoneInfo> {
        return this.phoneInfo
    }

    fun getAllReady(): LiveData<Boolean> {
        return this.allReady
    }

    @Subscribe
    fun onAuthenticationIDEvent(event: AuthenticationIDEvent) {
        this.idInfo.postValue(event.idInfo)
    }

    @Subscribe
    fun onBindEmailEvent(event: BindEmailEvent) {
        this.emailInfo.postValue(event.emailInfo)
    }

    @Subscribe
    fun onBindPhoneEvent(event: BindPhoneEvent) {
        this.phoneInfo.postValue(event.phoneInfo)
    }

    fun init() {
        if (this.idInfo.value != null) {
            return
        }

        viewModelScope.launch {
            var allReady = true

            val idInfo = this@UserViewModel.localUserService.getIDInfo()
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
            } else {
                execute()
            }
        }
    }

    override suspend fun realExecute(
        action: Int,
        vararg params: Any,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口

        // test code
        delay(5000)

        val idInfo = getIdInfo().value!!
        if (!idInfo.isAuthenticatedID()) {
            idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNAUTHORIZED
            this.localUserService.setIDInfo(idInfo)

            this.idInfo.postValue(idInfo)
        }

        val emailInfo = getEmailInfo().value!!
        if (!emailInfo.isBoundEmail()) {
            emailInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            this.localUserService.setEmailInfo(emailInfo)

            this.emailInfo.postValue(emailInfo)
        }

        val phoneInfo = getPhoneInfo().value!!
        if (!phoneInfo.isBoundPhone()) {
            phoneInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            this.localUserService.setPhoneInfo(phoneInfo)

            this.phoneInfo.postValue(phoneInfo)
        }

        this.allReady.postValue(
            idInfo.isAuthenticatedID()
                    && emailInfo.isBoundEmail()
                    && phoneInfo.isBoundPhone()
        )

        onSuccess.invoke()
    }
}
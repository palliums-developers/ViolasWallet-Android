package com.violas.wallet.ui.main.me

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
class MeViewModel : BaseViewModel() {

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    val idInfo = MutableLiveData<IDInfo>()
    val emailInfo = MutableLiveData<EmailInfo>()
    val phoneInfo = MutableLiveData<PhoneInfo>()

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
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
        viewModelScope.launch {
            val idInfo = localUserService.getIDInfo()
            this@MeViewModel.idInfo.postValue(idInfo)

            val emailInfo = localUserService.getEmailInfo()
            this@MeViewModel.emailInfo.postValue(emailInfo)

            val phoneInfo = localUserService.getPhoneInfo()
            this@MeViewModel.phoneInfo.postValue(phoneInfo)

            if (idInfo.idAuthenticationStatus == IDAuthenticationStatus.UNKNOWN
                || emailInfo.accountBindingStatus == AccountBindingStatus.UNKNOWN
                || phoneInfo.accountBindingStatus == AccountBindingStatus.UNKNOWN
            ) {
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

        val idInfo = this.idInfo.value!!
        if (!idInfo.isAuthenticatedID()) {
            idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNAUTHORIZED
            localUserService.setIDInfo(idInfo)
            this.idInfo.postValue(idInfo)
        }

        val emailInfo = this.emailInfo.value!!
        if (!emailInfo.isBoundEmail()) {
            emailInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            localUserService.setEmailInfo(emailInfo)
            this.emailInfo.postValue(emailInfo)
        }

        val phoneInfo = this.phoneInfo.value!!
        if (!phoneInfo.isBoundPhone()) {
            phoneInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            localUserService.setPhoneInfo(phoneInfo)
            this.phoneInfo.postValue(phoneInfo)
        }

        onSuccess.invoke()
    }
}
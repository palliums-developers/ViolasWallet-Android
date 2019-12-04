package com.violas.wallet.ui.main

import android.util.Log
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
import java.util.concurrent.atomic.AtomicBoolean

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

    companion object {
        private const val ACTION_INIT = 0x123
    }

    private lateinit var currentAccount: AccountDO

    private val ssoService by lazy {
        DataRepository.getSSOService()
    }

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    private val idInfoLiveData = MutableLiveData<Pair<IDInfo, LoadState>>()
    private val emailInfoLiveData = MutableLiveData<Pair<EmailInfo, LoadState>>()
    private val phoneInfoLiveData = MutableLiveData<Pair<PhoneInfo, LoadState>>()

    /**
     * 表示身份已认证，邮箱已绑定，手机已绑定
     */
    private val allReadyLiveData = MutableLiveData<Boolean>()

    private val initFlag = AtomicBoolean(false)

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun idInfo(): IDInfo? {
        return idInfoLiveData.value?.first
    }

    fun getIdInfo(): IDInfo? {
        synchronized(lock) {
            return idInfo()
        }
    }

    fun getIdInfoLiveData(): LiveData<Pair<IDInfo, LoadState>> {
        synchronized(lock) {
            return idInfoLiveData
        }
    }

    private fun emailInfo(): EmailInfo? {
        return emailInfoLiveData.value?.first
    }

    fun getEmailInfo(): EmailInfo? {
        synchronized(lock) {
            return emailInfoLiveData.value?.first
        }
    }

    fun getEmailInfoLiveData(): LiveData<Pair<EmailInfo, LoadState>> {
        synchronized(lock) {
            return emailInfoLiveData
        }
    }

    private fun phoneInfo(): PhoneInfo? {
        return phoneInfoLiveData.value?.first
    }

    fun getPhoneInfo(): PhoneInfo? {
        synchronized(lock) {
            return phoneInfoLiveData.value?.first
        }
    }

    fun getPhoneInfoLiveData(): LiveData<Pair<PhoneInfo, LoadState>> {
        synchronized(lock) {
            return phoneInfoLiveData
        }
    }

    fun getAllReadyLiveData(): LiveData<Boolean> {
        synchronized(lock) {
            return allReadyLiveData
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onAuthenticationIDEvent(event: AuthenticationIDEvent) {
        synchronized(lock) {
            idInfoLiveData.postValue(Pair(event.idInfo, LoadState.IDLE))

            val emailInfo = emailInfo()
            val phoneInfo = phoneInfo()
            if (emailInfo != null && emailInfo.isBoundEmail()
                && phoneInfo != null && phoneInfo.isBoundPhone()
            ) {
                allReadyLiveData.postValue(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onBindEmailEvent(event: BindEmailEvent) {
        synchronized(lock) {
            emailInfoLiveData.postValue(Pair(event.emailInfo, LoadState.IDLE))

            val idInfo = idInfo()
            val phoneInfo = phoneInfo()
            if (idInfo != null && idInfo.isAuthenticatedID()
                && phoneInfo != null && phoneInfo.isBoundPhone()
            ) {
                allReadyLiveData.postValue(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onBindPhoneEvent(event: BindPhoneEvent) {
        synchronized(lock) {
            phoneInfoLiveData.postValue(Pair(event.phoneInfo, LoadState.IDLE))

            val idInfo = idInfo()
            val emailInfo = emailInfo()
            if (idInfo != null && idInfo.isAuthenticatedID()
                && emailInfo != null && emailInfo.isBoundEmail()
            ) {
                allReadyLiveData.postValue(true)
            }
        }
    }

    fun init() {
        if (initFlag.get()) {
            return
        }

        initFlag.set(true)

        // 通知开始加载，此时外部获取用户信息为空
        loadState.postValue(LoadState.RUNNING)

        viewModelScope.launch(Dispatchers.IO) {
            synchronized(lock) {
                var allReady = true

                val idInfo = localUserService.getIDInfo()
                var infoLoadState = LoadState.IDLE
                if (!idInfo.isAuthenticatedID()) {
                    // 身份认证状态不为已认证先当作未知状态
                    idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNKNOWN
                    allReady = false
                    infoLoadState = LoadState.RUNNING
                }
                idInfoLiveData.postValue(Pair(idInfo, infoLoadState))

                val emailInfo = localUserService.getEmailInfo()
                infoLoadState = LoadState.IDLE
                if (!emailInfo.isBoundEmail()) {
                    // 邮箱绑定状态不为已绑定先当作未知状态
                    emailInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
                    allReady = false
                    infoLoadState = LoadState.RUNNING
                }
                emailInfoLiveData.postValue(Pair(emailInfo, infoLoadState))

                val phoneInfo = localUserService.getPhoneInfo()
                infoLoadState = LoadState.IDLE
                if (!phoneInfo.isBoundPhone()) {
                    // 手机绑定状态不为已绑定先当作未知状态
                    phoneInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
                    allReady = false
                    infoLoadState = LoadState.RUNNING
                }
                phoneInfoLiveData.postValue(Pair(phoneInfo, infoLoadState))

                currentAccount = AccountManager().currentAccount()

                if (allReady) {
                    allReadyLiveData.postValue(true)
                    loadState.postValue(LoadState.SUCCESS)
                    return@launch
                }
            }

            // 为了loading的连贯性，此处没有调用execute，而是直接调用的realExecute
            // 注意execute内部处理了异常，这里需要处理异常情况
            try {

                realExecute(ACTION_INIT)

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

        if (action != ACTION_INIT) {
            postStateIfNotReady(LoadState.RUNNING)
        }

        // 加载太快动画效果不好看
        delay(500)

        // 从服务器获取用户信息
        val walletAddress = currentAccount.address
        val userInfoDTO = try {
            ssoService.loadUserInfo(walletAddress).data
        } catch (e: Exception) {

            postStateIfNotReady(LoadState.failure(e))

            throw e
        }

        synchronized(lock) {
            // 解析存储分发身份信息
            val idInfo = idInfo()!!
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
            localUserService.setIDInfo(idInfo)
            idInfoLiveData.postValue(Pair(idInfo, LoadState.SUCCESS))

            // 解析存储分发邮箱信息
            val emailInfo = emailInfo()!!
            if (userInfoDTO == null
                || userInfoDTO.emailAddress.isNullOrEmpty()
            ) {
                emailInfo.emailAddress = ""
                emailInfo.accountBindingStatus = AccountBindingStatus.UNBOUND
            } else {
                emailInfo.emailAddress = userInfoDTO.emailAddress
                emailInfo.accountBindingStatus = AccountBindingStatus.BOUND
            }
            localUserService.setEmailInfo(emailInfo)
            emailInfoLiveData.postValue(Pair(emailInfo, LoadState.SUCCESS))

            // 解析存储分发手机信息
            val phoneInfo = phoneInfo()!!
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
            localUserService.setPhoneInfo(phoneInfo)
            phoneInfoLiveData.postValue(Pair(phoneInfo, LoadState.SUCCESS))

            // 分发申请发行准备进度
            allReadyLiveData.postValue(
                idInfo.isAuthenticatedID()
                        && emailInfo.isBoundEmail()
                        && phoneInfo.isBoundPhone()
            )
        }
    }

    private fun postStateIfNotReady(loadState: LoadState) {
        synchronized(lock) {
            val idInfo = idInfo()!!
            if (!idInfo.isAuthenticatedID()) {
                idInfoLiveData.postValue(Pair(idInfo, loadState))
            }

            val emailInfo = emailInfo()!!
            if (!emailInfo.isBoundEmail()) {
                emailInfoLiveData.postValue(Pair(emailInfo, loadState))
            }

            val phoneInfo = phoneInfo()!!
            if (!phoneInfo.isBoundPhone()) {
                phoneInfoLiveData.postValue(Pair(phoneInfo, loadState))
            }
        }
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        val idInfo = idInfo()
        val emailInfo = emailInfo()
        val phoneInfo = phoneInfo()

        return (idInfo != null && !idInfo.isAuthenticatedID())
                || (emailInfo != null && !emailInfo.isBoundEmail())
                || (phoneInfo != null && !phoneInfo.isBoundPhone())
    }

    override fun checkNetworkBeforeExecute(): Boolean {
        return false
    }
}
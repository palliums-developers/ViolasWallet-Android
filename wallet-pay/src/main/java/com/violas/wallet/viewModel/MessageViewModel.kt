package com.violas.wallet.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.extensions.logDebug
import com.palliums.extensions.logError
import com.palliums.utils.CustomMainScope
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.ChangeLanguageEvent
import com.violas.wallet.event.ClearUnreadMessagesEvent
import com.violas.wallet.event.ReadOneSystemMsgEvent
import com.violas.wallet.event.ReadOneTransactionMsgEvent
import com.violas.wallet.repository.DataRepository
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by elephant on 1/25/21 3:48 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MessageViewModel : ViewModel(), CoroutineScope by CustomMainScope() {

    companion object {
        private const val TAG = "MessageViewModel"

        fun getInstance(): MessageViewModel {
            val context = ContextProvider.getContext().applicationContext
            return ViewModelProvider(context as App).get(MessageViewModel::class.java)
        }
    }

    val unreadMsgNumLiveData = MutableLiveData<Long>()
    val unreadSysMsgNumLiveData = MutableLiveData<Long>()
    val unreadTxnMsgNumLiveData = MutableLiveData<Long>()

    private val accountManager by lazy { WalletAppViewModel.getViewModelInstance().mAccountManager }
    private val messageService by lazy { DataRepository.getMessageService() }
    private val lock by lazy { Any() }

    private var token: String? = null
    private var pushToken: String? = null
    private var syncUnreadMsgNumJob: Job? = null
    private var syncPushDeviceInfoJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        launch {
            token = accountManager.getToken()
            WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData.observeForever {
                if (!it) {
                    onDeleteWallet()
                }
                getPushTokenAndSyncPushDeviceInfo()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun getPushTokenAndSyncPushDeviceInfo() {
        val cachedPushToken = getPushToken()
        if (!cachedPushToken.isNullOrBlank()) {
            syncPushDeviceInfo(true)
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val pushToken = it.result
                it.logDebug(TAG) { "get push token successful, token = $pushToken" }
                if (!pushToken.isNullOrBlank()) {
                    setPushToken(pushToken)
                }
            } else {
                logError(TAG) { "get push token failed, ${it.exception?.toString()}" }
            }
            syncPushDeviceInfo(true)
        }
    }

    fun setPushToken(pushToken: String) {
        synchronized(lock) {
            this.pushToken = pushToken
        }
    }

    private fun getPushToken(): String? {
        synchronized(lock) {
            return pushToken
        }
    }

    suspend fun fetchToken(): String {
        val cachedToken = getToken()
        if (!cachedToken.isNullOrBlank()) {
            return cachedToken
        }

        val violasAccount =
            accountManager.getIdentityByCoinType(getViolasCoinType().coinNumber())
        val token = messageService.registerPushDeviceInfo(
            address = violasAccount?.address ?: "",
            pushToken = getPushToken()
        )
        setToken(token)
        return token
    }

    private fun setToken(token: String) {
        synchronized(lock) {
            this.token = token
            accountManager.setToken(token)
        }
    }

    private fun getToken(): String? {
        synchronized(lock) {
            return token
        }
    }

    fun syncPushDeviceInfo(first: Boolean = false) {
        synchronized(lock) {
            if (syncPushDeviceInfoJob != null) return
        }

        syncPushDeviceInfoJob = launch(Dispatchers.IO) {
            if (first)
                delay(1500)

            val result = try {
                val violasAccount =
                    accountManager.getIdentityByCoinType(getViolasCoinType().coinNumber())
                val cachedToken = getToken()
                if (cachedToken.isNullOrBlank()) {
                    val token = messageService.registerPushDeviceInfo(
                        address = violasAccount?.address ?: "",
                        pushToken = getPushToken()
                    )
                    setToken(token)
                } else {
                    messageService.modifyPushDeviceInfo(
                        token = cachedToken,
                        address = violasAccount?.address ?: "",
                        pushToken = getPushToken()
                    )
                }
                true
            } catch (e: Exception) {
                logError(TAG) { "sync push device info failed, ${e.message}" }
                false
            }

            syncUnreadMsgNum()

            synchronized(lock) {
                syncPushDeviceInfoJob = null
            }

            if (!result) {
                delay(1000 * 60 * 10)
                syncPushDeviceInfo()
            }
        }
    }

    fun syncUnreadMsgNum() {
        synchronized(lock) {
            if (syncUnreadMsgNumJob != null) return
        }

        syncUnreadMsgNumJob = launch {
            delay(500)

            val unreadMsgNumber = withContext(Dispatchers.IO) {
                try {
                    val token = fetchToken()
                    messageService.getUnreadMsgNumber(token)
                } catch (e: Exception) {
                    logError(TAG) { "get unread message number failed, ${e.message}" }
                    null
                }
            }

            synchronized(lock) {
                unreadMsgNumber?.let {
                    if (WalletAppViewModel.getViewModelInstance().isExistsAccount()) {
                        unreadMsgNumLiveData.value = it.txn + it.sys
                        unreadTxnMsgNumLiveData.value = it.txn
                    } else {
                        unreadMsgNumLiveData.value = it.sys
                        unreadTxnMsgNumLiveData.value = 0
                    }
                    unreadSysMsgNumLiveData.value = it.sys
                }

                syncUnreadMsgNumJob = null
            }
        }
    }

    private fun onDeleteWallet() {
        launch {
            synchronized(lock) {
                val unreadMsgNum = unreadMsgNumLiveData.value ?: 0
                val unreadTxnMsgNum = unreadTxnMsgNumLiveData.value ?: 0
                if (unreadTxnMsgNum > 0) {
                    unreadMsgNumLiveData.value = if (unreadMsgNum > unreadTxnMsgNum)
                        unreadMsgNum - unreadTxnMsgNum
                    else
                        0
                    unreadTxnMsgNumLiveData.value = 0
                }
            }
        }
    }

    @Subscribe
    fun onClearUnreadMessagesEvent(event: ClearUnreadMessagesEvent) {
        launch {
            synchronized(lock) {
                try {
                    if (syncUnreadMsgNumJob != null) {
                        syncUnreadMsgNumJob!!.cancel()
                        syncUnreadMsgNumJob = null
                    }
                } catch (e: Exception) {
                }

                unreadMsgNumLiveData.value = 0
                unreadSysMsgNumLiveData.value = 0
                unreadTxnMsgNumLiveData.value = 0
            }
        }
    }

    @Subscribe
    fun onReadOneSystemMsgEvent(event: ReadOneSystemMsgEvent) {
        launch {
            synchronized(lock) {
                val unreadMsgNum = unreadMsgNumLiveData.value ?: 0
                val unreadSysMsgNum = unreadSysMsgNumLiveData.value ?: 0
                unreadMsgNumLiveData.value = if (unreadMsgNum <= 0)
                    0
                else
                    unreadMsgNum - 1
                unreadSysMsgNumLiveData.value = if (unreadSysMsgNum <= 0)
                    0
                else
                    unreadSysMsgNum - 1
            }
        }
    }

    @Subscribe
    fun onReadOneTransactionMsgEvent(event: ReadOneTransactionMsgEvent) {
        launch {
            synchronized(lock) {
                val unreadMsgNum = unreadMsgNumLiveData.value ?: 0
                val unreadTxnMsgNum = unreadTxnMsgNumLiveData.value ?: 0
                unreadMsgNumLiveData.value = if (unreadMsgNum <= 0)
                    0
                else
                    unreadMsgNum - 1
                unreadTxnMsgNumLiveData.value = if (unreadTxnMsgNum <= 0)
                    0
                else
                    unreadTxnMsgNum - 1
            }
        }
    }

    @Subscribe
    fun onChangeLanguageEvent(event: ChangeLanguageEvent) {
        getPushTokenAndSyncPushDeviceInfo()
    }
}
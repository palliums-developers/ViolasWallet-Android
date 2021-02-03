package com.violas.wallet.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.extensions.logDebug
import com.palliums.extensions.logError
import com.palliums.utils.CustomMainScope
import com.quincysx.crypto.CoinTypes
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

    private val accountStorage by lazy { DataRepository.getAccountStorage() }
    private val messageService by lazy { DataRepository.getMessageService() }
    private val lock by lazy { Any() }

    private var syncUnreadMsgNumJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData.observeForever {
            getPushTokenAndRegisterPushDevice()
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun getPushTokenAndRegisterPushDevice() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result?.token
                logDebug(TAG) {
                    "getToken. isSuccessful = true, token = $token"
                }
                if (!token.isNullOrBlank()) {
                    registerPushDevice(token, true)
                }
            } else {
                logError(TAG) {
                    "getToken. isSuccessful = false, exception = ${it.exception?.toString()}"
                }
            }
        }
    }

    fun registerPushDevice(token: String, first: Boolean = false) {
        launch(Dispatchers.IO) {
            if (first)
                delay(3000)

            val result = try {
                val violasAccount = accountStorage.findByCoinType(CoinTypes.Violas.coinType())
                messageService.registerPushDevice(violasAccount?.address ?: "", token)
                true
            } catch (e: Exception) {
                logError(TAG) { "register push device failed, ${e.message}" }
                false
            }

            if (!result) {
                delay(1000 * 60 * 10)
                registerPushDevice(token)
            }
        }
    }

    fun syncUnreadMsgNum() {
        if (syncUnreadMsgNumJob != null) return

        syncUnreadMsgNumJob = launch {
            delay(500)

            withContext(Dispatchers.IO) {
                try {
                    val violasAccount = accountStorage.findByCoinType(CoinTypes.Violas.coinType())
                    // TODO 从后台获取未读消息数
                    messageService
                } catch (e: Exception) {
                    logError(TAG) { "load message number failed, ${e.message}" }
                }
            }

            synchronized(lock) {
                unreadMsgNumLiveData.value = 9
                unreadSysMsgNumLiveData.value = 1
                unreadTxnMsgNumLiveData.value = 8
            }

            syncUnreadMsgNumJob = null
        }
    }

    @Subscribe
    fun onClearUnreadMessagesEvent(event: ClearUnreadMessagesEvent) {
        launch {
            try {
                if (syncUnreadMsgNumJob != null) {
                    syncUnreadMsgNumJob!!.cancel()
                    syncUnreadMsgNumJob = null
                }
            } catch (e: Exception) {
            }

            synchronized(lock) {
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
                val unreadMsgNum = unreadMsgNumLiveData.value
                val unreadSysMsgNum = unreadSysMsgNumLiveData.value
                unreadMsgNumLiveData.value = if (unreadMsgNum ?: 0 <= 0)
                    0
                else
                    unreadMsgNum!! - 1
                unreadSysMsgNumLiveData.value = if (unreadSysMsgNum ?: 0 <= 0)
                    0
                else
                    unreadSysMsgNum!! - 1
            }
        }
    }

    @Subscribe
    fun onReadOneTransactionMsgEvent(event: ReadOneTransactionMsgEvent) {
        launch {
            synchronized(lock) {
                val unreadMsgNum = unreadMsgNumLiveData.value
                val unreadTxnMsgNum = unreadTxnMsgNumLiveData.value
                unreadMsgNumLiveData.value = if (unreadMsgNum ?: 0 <= 0)
                    0
                else
                    unreadMsgNum!! - 1
                unreadTxnMsgNumLiveData.value = if (unreadTxnMsgNum ?: 0 <= 0)
                    0
                else
                    unreadTxnMsgNum!! - 1
            }
        }
    }
}
package com.violas.wallet.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.extensions.logDebug
import com.palliums.extensions.logError
import com.palliums.extensions.logInfo
import com.palliums.utils.CustomMainScope
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.event.ChangeLanguageEvent
import com.violas.wallet.event.ClearUnreadMessagesEvent
import com.violas.wallet.event.ReadOneSystemMsgEvent
import com.violas.wallet.event.ReadOneTransactionMsgEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
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

    private val messageService by lazy { DataRepository.getMessageService() }
    private val lock by lazy { Any() }

    private var token: String? = null
    private var pushToken: String? = null
    private var syncUnreadMsgNumJob: Job? = null
    private var syncPushDeviceInfoJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        launch {
            token = AccountManager.getAppToken()
            WalletAppViewModel.getInstance().mExistsAccountLiveData.observeForever {
                if (!it) {
                    onDeleteWallet()
                }
                getPushTokenAndSyncPushDeviceInfo(true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun getPushTokenAndSyncPushDeviceInfo(syncUnreadMsgNum: Boolean) {
        syncPushDeviceInfo(true, syncUnreadMsgNum)

        if (!getPushToken().isNullOrBlank()) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val pushToken = it.result
                logDebug(TAG) { "get push token successful, token = $pushToken" }
                if (!pushToken.isNullOrBlank()) {
                    setPushToken(pushToken)
                    syncPushDeviceInfo(delayStart = false, syncUnreadMsgNum = false)
                }
            } else {
                logError(TAG) { "get push token failed, ${it.exception?.toString()}" }
            }
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

        val token = messageService.registerPushDeviceInfo(
            address = AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())?.address,
            pushToken = getPushToken(),
            language = MultiLanguageUtility.getInstance().localTag.toLowerCase()
        )
        setToken(token)
        return getToken()!!
    }

    private fun setToken(token: String) {
        synchronized(lock) {
            if (this.token.isNullOrBlank()) {
                this.token = token
                AccountManager.setAppToken(token)
            }
        }
    }

    private fun getToken(): String? {
        synchronized(lock) {
            return token
        }
    }

    fun syncPushDeviceInfo(delayStart: Boolean, syncUnreadMsgNum: Boolean = false) {
        synchronized(lock) {
            if (syncPushDeviceInfoJob != null) return
        }

        syncPushDeviceInfoJob = launch(Dispatchers.IO) {
            if (delayStart)
                delay(1000)

            val result = run {
                try {
                    val violasAddress =
                        AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())?.address
                    val language = MultiLanguageUtility.getInstance().localTag.toLowerCase()
                    val pushToken = getPushToken()
                    val cachedToken = getToken()

                    if (cachedToken.isNullOrBlank()) {
                        return@run try {
                            val token = messageService.registerPushDeviceInfo(
                                address = violasAddress,
                                pushToken = pushToken,
                                language = language
                            )
                            setToken(token)

                            if (isDeviceInfoChange(violasAddress, pushToken, language)) {
                                logInfo(TAG) { "The registered push device info changes and needs to be updated" }
                                0
                            } else {
                                1
                            }
                        } catch (e: Exception) {
                            logError(TAG) { "register push device info failed, ${e.message}" }
                            -1
                        }
                    }

                    return@run try {
                        messageService.modifyPushDeviceInfo(
                            token = cachedToken,
                            address = violasAddress,
                            pushToken = pushToken,
                            language = language
                        )

                        if (isDeviceInfoChange(violasAddress, pushToken, language)) {
                            logInfo(TAG) { "The modified push device info changes and needs to be updated" }
                            0
                        } else {
                            1
                        }
                    } catch (e: Exception) {
                        logError(TAG) { "modify push device info failed, ${e.message}" }
                        -2
                    }
                } catch (e: Exception) {
                    logError(TAG) { "sync push device info failed, ${e.message}" }
                    return@run -3
                }
            }

            synchronized(lock) {
                syncPushDeviceInfoJob = null
            }

            if (syncUnreadMsgNum && result != -1) {
                syncUnreadMsgNum()
            }

            if (result <= 0) {
                if (result < 0)
                    delay(1000 * 60 * 3)
                syncPushDeviceInfo(delayStart = false, syncUnreadMsgNum = false)
            }
        }
    }

    private fun isDeviceInfoChange(
        address: String?,
        pushToken: String?,
        language: String
    ): Boolean {
        val nowAddress =
            AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())?.address
        val nowPushToken = getPushToken()
        val nowLanguage =
            MultiLanguageUtility.getInstance().localTag.toLowerCase()
        return address != nowAddress || pushToken != nowPushToken || language != nowLanguage
    }

    fun syncUnreadMsgNum() {
        synchronized(lock) {
            if (syncUnreadMsgNumJob != null) return
        }

        syncUnreadMsgNumJob = launch(Dispatchers.IO) {
            val violasAddress =
                AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())?.address

            val unreadMsgNumber = try {
                val token = fetchToken()
                messageService.getUnreadMsgNumber(token)
            } catch (e: Exception) {
                logError(TAG) { "get unread message number failed, ${e.message}" }
                null
            }

            val nowViolasAddress =
                AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())?.address

            withContext(Dispatchers.Main) {
                synchronized(lock) {
                    if (violasAddress == nowViolasAddress && unreadMsgNumber != null) {
                        if (WalletAppViewModel.getInstance().isExistsAccount()) {
                            unreadMsgNumLiveData.value = unreadMsgNumber.txn + unreadMsgNumber.sys
                            unreadTxnMsgNumLiveData.value = unreadMsgNumber.txn
                        } else {
                            unreadMsgNumLiveData.value = unreadMsgNumber.sys
                            unreadTxnMsgNumLiveData.value = 0
                        }
                        unreadSysMsgNumLiveData.value = unreadMsgNumber.sys
                    }

                    syncUnreadMsgNumJob = null
                }

                if (violasAddress != nowViolasAddress) {
                    syncUnreadMsgNum()
                }
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
        getPushTokenAndSyncPushDeviceInfo(false)
    }
}
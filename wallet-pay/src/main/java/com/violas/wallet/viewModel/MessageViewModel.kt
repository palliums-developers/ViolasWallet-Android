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
import com.violas.wallet.repository.DataRepository
import kotlinx.coroutines.*

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

    val msgTotalNumLiveData = MutableLiveData<Long>()
    val systemMsgNumLiveData = MutableLiveData<Long>()
    val transactionMsgNumLiveData = MutableLiveData<Long>()

    init {
        WalletAppViewModel.getViewModelInstance().mExistsAccountLiveData.observeForever {
            getTokenAndRegisterDevice()
        }
    }

    private fun getTokenAndRegisterDevice() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result?.token
                logDebug(TAG) {
                    "getToken. isSuccessful = true, token = $token"
                }
                if (!token.isNullOrBlank()) {
                    registerDevice(token)
                }
            } else {
                logError(TAG) {
                    "getToken. isSuccessful = false, exception = ${it.exception?.toString()}"
                }
            }
        }
    }

    fun registerDevice(token: String) {
        launch(Dispatchers.IO) {
            val result = try {
                val violasAccount = DataRepository.getAccountStorage()
                    .findByCoinType(CoinTypes.Violas.coinType())
                val messageService = DataRepository.getMessageService()
                messageService.registerDevice(violasAccount!!.address, token)
                true
            } catch (e: Exception) {
                logError(TAG) { "register device failed, ${e.message}" }
                false
            }

            if (!result) {
                delay(1000 * 60 * 10)
                registerDevice(token)
            }
        }
    }

    fun loadMsgNum() {
        launch {
            withContext(Dispatchers.IO) {
                try {
                    val violasAccount = DataRepository.getAccountStorage()
                        .findByCoinType(CoinTypes.Violas.coinType())
                    val messageService = DataRepository.getMessageService()
                } catch (e: Exception) {
                    logError(TAG) { "load message number failed, ${e.message}" }
                }
            }

            msgTotalNumLiveData.value = 3
            systemMsgNumLiveData.value = 2
            transactionMsgNumLiveData.value = 1
        }
    }
}
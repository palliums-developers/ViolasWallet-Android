package com.violas.wallet.ui.dexOrder.details

import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.net.LoadState
import com.palliums.net.postTipsMessage
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexOrderTradeDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-12-17 10:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderDetailsViewModel(
    private val version: String
) : PagingViewModel<DexOrderTradeDTO>() {

    private val lock by lazy { Any() }
    val loadState by lazy { EnhancedMutableLiveData<LoadState>() }
    val tipsMessage by lazy { EnhancedMutableLiveData<String>() }

    private val dexService by lazy {
        DataRepository.getDexService()
    }

    private val exchangeManager by lazy {
        ExchangeManager()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DexOrderTradeDTO>, Any?) -> Unit
    ) {
        val response = dexService.getOrderTrades(
            version, pageSize, pageNumber
        )

        onSuccess.invoke(response.data ?: emptyList(), null)
    }

    fun revokeOrder(
        walletAccount: AccountDO,
        password: ByteArray,
        dexOrder: DexOrderDTO,
        onCheckPassword: (Boolean) -> Unit,
        onRevokeSuccess: () -> Unit
    ): Boolean {
        synchronized(lock) {
            if (loadState.value?.peekData()?.status == LoadState.Status.RUNNING) {
                return false
            } else if (!dexOrder.isOpen()) {
                return false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decryptPrivateKey =
                    SimpleSecurity.instance(ContextProvider.getContext())
                        .decrypt(password, walletAccount.privateKey)
                if (decryptPrivateKey == null) {
                    tipsMessage.postValueSupport(getString(R.string.hint_password_error))
                    onCheckPassword.invoke(false)
                    return@launch
                }

                onCheckPassword.invoke(true)
                loadState.postValueSupport(LoadState.RUNNING)

                val result = exchangeManager.revokeOrder(decryptPrivateKey, dexOrder, dexService)
                if (result) {
                    withContext(Dispatchers.Main) {
                        onRevokeSuccess.invoke()
                    }
                }

                synchronized(lock) {
                    loadState.postValueSupport(LoadState.SUCCESS)
                    if (!result) {
                        tipsMessage.postValueSupport(getString(R.string.tips_revoke_failure))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                synchronized(lock) {
                    loadState.postValueSupport(LoadState.failure(e))
                    postTipsMessage(tipsMessage, e)
                }
            }
        }
        return true
    }
}
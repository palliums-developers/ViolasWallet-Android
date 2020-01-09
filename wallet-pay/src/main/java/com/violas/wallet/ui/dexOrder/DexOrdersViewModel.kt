package com.violas.wallet.ui.dexOrder

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2019-12-06 17:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersViewModel(
    private val accountAddress: String,
    @DexOrderState
    private val orderState: String?,
    private val tokenGiveAddress: String? = null,
    private val tokenGetAddress: String? = null
) : PagingViewModel<DexOrderDTO>() {

    private val lock by lazy { Any() }
    val loadState by lazy { EnhancedMutableLiveData<LoadState>() }
    val tipsMessage by lazy { EnhancedMutableLiveData<String>() }

    private val dexService by lazy {
        DataRepository.getDexService()
    }
    private val exchangeManager by lazy {
        ExchangeManager()
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

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DexOrderDTO>, Any?) -> Unit
    ) {

        /*delay(1000)
        val fakeOrdersData = fakeOrdersData()
        onSuccess.invoke(fakeOrdersData, fakeOrdersData.last().version)*/

        val response = dexService.getMyOrders(
            accountAddress = accountAddress,
            pageSize = pageSize,
            lastVersion = if (pageKey == null) null else pageKey as String,
            orderState = orderState,
            tokenGiveAddress = tokenGiveAddress,
            tokenGetAddress = tokenGetAddress
        )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        onSuccess.invoke(response.data!!, response.data!!.last().version)
    }

    private fun fakeOrdersData(): List<DexOrderDTO> {
        val list = mutableListOf<DexOrderDTO>()
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_0",
                user = accountAddress,
                state = getState(1),
                amountGive = "1",
                tokenGiveAddress = tokenGiveAddress
                    ?: "0x05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054",
                tokenGiveSymbol = "ABCUSD",
                tokenGivePrice = 1.0,
                amountGet = "100",
                tokenGetAddress = tokenGetAddress
                    ?: "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                tokenGetSymbol = "HIJUSD",
                tokenGetPrice = 1.0,
                amountFilled = "1",
                version = "1",
                updateVersion = "1",
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_1",
                user = accountAddress,
                state = getState(2),
                amountGive = "1",
                tokenGiveAddress = tokenGiveAddress
                    ?: "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                tokenGiveSymbol = "HIJUSD",
                tokenGivePrice = 1.0,
                amountGet = "1277",
                tokenGetAddress = tokenGetAddress
                    ?: "0xe90e4f077bef23b32a6694a18a1fa34244532400869e4e8c87ce66d0b6c004bd",
                tokenGetSymbol = "BCDCAD",
                tokenGetPrice = 1.0,
                amountFilled = "1",
                version = "2",
                updateVersion = "2",
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_2",
                user = accountAddress,
                state = getState(3),
                amountGive = "1",
                tokenGiveAddress = tokenGiveAddress
                    ?: "0xf013ea4acf944fa6edafe01fae10713d13928ca5dff9e809dbcce8b12c2c45f1",
                tokenGiveSymbol = "CIYHKD",
                tokenGivePrice = 1.0,
                amountGet = "1",
                tokenGetAddress = tokenGetAddress
                    ?: "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
                tokenGetSymbol = "XYASGD",
                tokenGetPrice = 1.0,
                amountFilled = "1",
                version = "3",
                updateVersion = "3",
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        return list
    }

    private fun getState(index: Int): String {
        return when (orderState) {
            DexOrderState.OPEN -> "OPEN"
            DexOrderState.CANCELED -> "CANCELED"
            DexOrderState.FILLED -> "FILLED"
            DexOrderState.FINISHED -> {
                when (index / 2) {
                    0 -> "CANCELED"
                    else -> "FILLED"
                }
            }

            else -> {
                when (index / 3) {
                    0 -> "OPEN"
                    1 -> "CANCELED"
                    else -> "FILLED"
                }
            }
        }
    }
}
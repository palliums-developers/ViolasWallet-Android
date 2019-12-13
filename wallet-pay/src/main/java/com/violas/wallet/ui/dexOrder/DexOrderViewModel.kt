package com.violas.wallet.ui.dexOrder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.ContextProvider
import com.palliums.net.LoadState
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexTokenCache
import com.violas.wallet.repository.http.dex.DexTokenPriceDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.wallet.KeyPair

/**
 * Created by elephant on 2019-12-06 17:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrderViewModel(
    private val accountAddress: String,
    @DexOrdersState
    private val orderState: String?,
    private val giveTokenAddress: String?,
    private val getTokenAddress: String?
) : PagingViewModel<DexOrderVO>() {

    private val lock: Any = Any()
    val loadState = MutableLiveData<LoadState>()
    val tipsMessage = MutableLiveData<String>()

    private val dexService by lazy {
        DataRepository.getDexService()
    }
    private val exchangeManager by lazy {
        ExchangeManager()
    }

    fun revokeOrder(
        account: AccountDO,
        password: ByteArray,
        dexOrderVO: DexOrderVO,
        onCheckPassword: (Boolean) -> Unit,
        onRevokeSuccess: () -> Unit
    ): Boolean {
        synchronized(lock) {
            if (loadState.value?.status == LoadState.Status.RUNNING) {
                return false
            } else if (!dexOrderVO.isOpen()) {
                return false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decryptPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
                    .decrypt(password, account.privateKey)
                if (decryptPrivateKey == null) {
                    tipsMessage.postValue(getString(R.string.hint_password_error))
                    onCheckPassword.invoke(false)
                    return@launch
                }

                onCheckPassword.invoke(true)
                loadState.postValue(LoadState.RUNNING)

                val account = Account(KeyPair.fromSecretKey(decryptPrivateKey!!))
                val result = exchangeManager.undoExchangeToken(
                    account,
                    dexOrderVO.dexOrderDTO.tokenGive,
                    dexOrderVO.dexOrderDTO.version
                )

                if (result) {
                    withContext(Dispatchers.Main) {
                        onRevokeSuccess.invoke()
                    }
                }

                synchronized(lock) {
                    loadState.postValue(LoadState.SUCCESS)
                    tipsMessage.postValue(
                        getString(
                            if (result)
                                R.string.tips_revoke_success
                            else
                                R.string.tips_revoke_failure
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()

                synchronized(lock) {
                    loadState.postValue(LoadState.failure(e))
                    tipsMessage.postValue(e.message)
                }
            }
        }
        return true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DexOrderVO>, Any?) -> Unit
    ) {

        /*delay(1000)
        val fakeOrdersData = fakeOrdersData()
        val fakeTokensData = fakeTokensData()

        val fakeOrders = fakeOrdersData.map {
            val tokenGive = fakeTokensData[it.tokenGive] ?: error("give token not found")
            val tokenGet = fakeTokensData[it.tokenGet] ?: error("get token not found")

            DexOrderVO(
                dexOrderDTO = it,
                giveTokenName = tokenGive.name,
                giveTokenPrice = tokenGive.price,
                getTokenName = tokenGet.name,
                getTokenPrice = tokenGet.price
            )
        }

        onSuccess.invoke(fakeOrders, fakeOrders.last().dexOrderDTO.version)*/

        val response = dexService.getMyOrders(
            accountAddress = accountAddress,
            pageSize = pageSize.toString(),
            lastVersion = if (pageKey == null) null else pageKey as String,
            orderState = orderState,
            giveTokenAddress = giveTokenAddress,
            getTokenAddress = getTokenAddress
        )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val dexTokens = DexTokenCache.getDexTokens(dexService)
        val dexOrders = response.data!!.map {
            val tokenGive = dexTokens[it.tokenGive] ?: error("give token not found")
            val tokenGet = dexTokens[it.tokenGet] ?: error("get token not found")

            DexOrderVO(
                dexOrderDTO = it,
                giveTokenName = tokenGive.name,
                giveTokenPrice = tokenGive.price,
                getTokenName = tokenGet.name,
                getTokenPrice = tokenGet.price
            )
        }

        onSuccess.invoke(dexOrders, dexOrders.last().dexOrderDTO.version)
    }

    private fun fakeTokensData(): Map<String, DexTokenPriceDTO> {
        val list = mutableListOf<DexTokenPriceDTO>()
        list.add(
            DexTokenPriceDTO(
                address = "0x0000000000000000000000000000000000000000000000000000000000000000",
                name = "VToken",
                price = 100.toDouble()
            )
        )
        list.add(
            DexTokenPriceDTO(
                address = "0x05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054",
                name = "Xcoin",
                price = 100.toDouble()
            )
        )
        list.add(
            DexTokenPriceDTO(
                address = "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                name = "ABCUSD",
                price = 1.toDouble()
            )
        )
        list.add(
            DexTokenPriceDTO(
                address = "0xf013ea4acf944fa6edafe01fae10713d13928ca5dff9e809dbcce8b12c2c45f1",
                name = "XYZUSD",
                price = 1.toDouble()
            )
        )
        list.add(
            DexTokenPriceDTO(
                address = "0xe90e4f077bef23b32a6694a18a1fa34244532400869e4e8c87ce66d0b6c004bd",
                name = "DEFHKD",
                price = 0.1277
            )
        )
        list.add(
            DexTokenPriceDTO(
                address = "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
                name = "DEFCNY",
                price = 10.toDouble()
            )
        )

        val map = mutableMapOf<String, DexTokenPriceDTO>()
        list.forEach {
            map[it.address] = it
        }

        return map
    }

    private fun fakeOrdersData(): List<DexOrderDTO> {
        val list = mutableListOf<DexOrderDTO>()
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_0",
                user = accountAddress,
                state = getState(1),
                tokenGive = giveTokenAddress
                    ?: "0x05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054",
                amountGive = "1",
                tokenGet = getTokenAddress
                    ?: "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                amountGet = "100",
                amountFilled = "1",
                version = 1,
                updateVersion = 1,
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_1",
                user = accountAddress,
                state = getState(2),
                tokenGive = giveTokenAddress
                    ?: "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                amountGive = "1",
                tokenGet = getTokenAddress
                    ?: "0xe90e4f077bef23b32a6694a18a1fa34244532400869e4e8c87ce66d0b6c004bd",
                amountGet = "1277",
                amountFilled = "1",
                version = 2,
                updateVersion = 2,
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        list.add(
            DexOrderDTO(
                id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_2",
                user = accountAddress,
                state = getState(3),
                tokenGive = giveTokenAddress
                    ?: "0xf013ea4acf944fa6edafe01fae10713d13928ca5dff9e809dbcce8b12c2c45f1",
                amountGive = "1",
                tokenGet = getTokenAddress
                    ?: "0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095",
                amountGet = "0.1",
                amountFilled = "1",
                version = 3,
                updateVersion = 3,
                date = System.currentTimeMillis(),
                updateDate = System.currentTimeMillis()
            )
        )
        return list
    }

    private fun getState(index: Int): String {
        return when (orderState) {
            DexOrdersState.OPEN -> "OPEN"
            DexOrdersState.CANCELED -> "CANCELED"
            DexOrdersState.FILLED -> "FILLED"
            DexOrdersState.FINISHED -> {
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
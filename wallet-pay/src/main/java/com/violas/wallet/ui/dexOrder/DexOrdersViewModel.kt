package com.violas.wallet.ui.dexOrder

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.dex.DexOrderDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 2019-12-06 17:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersViewModel(
    private val accountAddress: String,
    @DexOrdersState
    private val orderState: String?,
    private val baseTokenAddress: String?,
    private val quoteTokenAddress: String?
) : PagingViewModel<DexOrderDTO>() {

    private val dexService by lazy {
        DataRepository.getDexService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DexOrderDTO>, Any?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {

        delay(1000)
        val fakeData = fakeData()
        onSuccess.invoke(fakeData, fakeData.last().version)

        /*try {
            val listResponse = dexService.getMyOrders(
                accountAddress = accountAddress,
                pageSize = pageSize.toString(),
                lastVersion = if (pageKey == null) "" else pageKey as String,
                orderState = orderState,
                baseTokenAddress = baseTokenAddress,
                quoteTokenAddress = quoteTokenAddress
            )

            if (listResponse.data.isNullOrEmpty()) {
                onSuccess.invoke(emptyList(), null)
                return
            }

            onSuccess.invoke(listResponse.data!!, listResponse.data!!.last().version)

        } catch (e: Exception) {
            onFailure.invoke(e)
        }*/
    }

    private suspend fun fakeData(): List<DexOrderDTO> {
        val list = mutableListOf<DexOrderDTO>()
        repeat(3) {
            list.add(
                DexOrderDTO(
                    id = "0xed652301d8cf1826ebf329520870fc3b6a39fdfc843500ddf9b9e21412323aad_$it",
                    user = accountAddress,
                    state = orderState ?: it.toString(),
                    tokenGet = baseTokenAddress
                        ?: "0x05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054",
                    amountGet = "${it}00",
                    tokenGive = quoteTokenAddress
                        ?: "0xb9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e",
                    amountGive = "${it}00",
                    version = it.toString(),
                    date = "2019-11-14T10:50:37",
                    updated = it.toString(),
                    availableVolume = "0.00$it",
                    amountFilled = "0"
                )
            )
        }
        return list
    }
}
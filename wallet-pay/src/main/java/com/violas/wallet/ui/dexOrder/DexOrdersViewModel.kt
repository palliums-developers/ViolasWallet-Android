package com.violas.wallet.ui.dexOrder

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.dex.DexOrderDTO

/**
 * Created by elephant on 2019-12-06 17:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DexOrdersViewModel(
    private val accountAddress: String,
    @DexOrdersState
    private val orderState: String
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

        try {
            val listResponse = dexService.getMyOrders(
                accountAddress = accountAddress,
                pageSize = pageSize.toString(),
                lastVersion = if (pageKey == null) "" else pageKey as String,
                orderState = orderState
            )

            if (listResponse.data.isNullOrEmpty()) {
                onSuccess.invoke(emptyList(), null)
                return
            }

            onSuccess.invoke(listResponse.data!!, listResponse.data!!.last().version)

        } catch (e: Exception) {
            onFailure.invoke(e)
        }
    }
}
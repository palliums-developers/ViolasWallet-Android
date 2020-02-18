package com.violas.wallet.ui.outsideExchange.orders

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import kotlinx.coroutines.delay

/**
 * Created by elephant on 2020-02-18 12:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingExchangeOrdersViewModel(
    private val accountAddress: String
) : PagingViewModel<MappingExchangeOrderVO>() {

    private val mappingExchangeService by lazy {
        DataRepository.getMappingExchangeService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<MappingExchangeOrderVO>, Any?) -> Unit
    ) {
        // TODO 对接接口
        delay(500)
        onSuccess.invoke(emptyList(), null)
    }
}
package com.violas.wallet.ui.mapping

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.mapping.MappingRecordDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/14 17:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingRecordViewModel : PagingViewModel<MappingRecordDTO>() {

    private val mappingService by lazy { DataRepository.getMappingService() }

    private lateinit var address: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<MappingRecordDTO>, Any?) -> Unit
    ) {
        val mappingRecords =
            mappingService.getMappingRecords(
                address, pageSize, (pageNumber - 1) * pageSize
            )
        onSuccess.invoke(mappingRecords ?: emptyList(), null)
    }
}
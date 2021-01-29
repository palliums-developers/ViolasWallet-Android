package com.violas.wallet.ui.mapping

import com.palliums.paging.PagingViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.mapping.MappingRecordDTO
import com.violas.wallet.utils.getBtcCoinType
import com.violas.wallet.utils.getLibraCoinType
import com.violas.wallet.utils.getViolasCoinType
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

    private lateinit var violasWalletAddress: String
    private lateinit var libraWalletAddress: String
    private lateinit var bitcoinWalletAddress: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(getViolasCoinType().coinType())
                ?: return@withContext false
        val libraAccount =
            AccountManager().getIdentityByCoinType(getLibraCoinType().coinType())
                ?: return@withContext false
        val bitcoinAccount =
            AccountManager().getIdentityByCoinType(getBtcCoinType().coinType())
                ?: return@withContext false

        violasWalletAddress = violasAccount.address
        libraWalletAddress = libraAccount.address
        bitcoinWalletAddress = bitcoinAccount.address
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
                violasWalletAddress,
                libraWalletAddress,
                bitcoinWalletAddress,
                pageSize,
                (pageNumber - 1) * pageSize
            )
        onSuccess.invoke(mappingRecords ?: emptyList(), null)
    }
}
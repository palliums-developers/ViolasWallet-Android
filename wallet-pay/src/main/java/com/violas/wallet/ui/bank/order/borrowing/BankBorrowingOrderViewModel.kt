package com.violas.wallet.ui.bank.order.borrowing

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.AccountBorrowingInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankBorrowingOrderViewModel : PagingViewModel<AccountBorrowingInfoDTO>() {

    private lateinit var address: String

    private val bankService by lazy { DataRepository.getBankService() }

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
        onSuccess: (List<AccountBorrowingInfoDTO>, Any?) -> Unit
    ) {
        val list = bankService.getAccountBorrowingInfos(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }

    private fun fakeData(): List<AccountBorrowingInfoDTO> {
        return mutableListOf(
            AccountBorrowingInfoDTO(
                "1",
                "VLSUSD",
                "00000000000000000000000000000000",
                "1001110000"
            ),
            AccountBorrowingInfoDTO(
                "2",
                "VLSEUR",
                "00000000000000000000000000000000",
                "1231410000"
            )
        )
    }
}
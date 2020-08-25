package com.violas.wallet.ui.bank.order.borrowing

import com.palliums.listing.ListingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.http.bank.CurrBorrowingDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankBorrowingOrderViewModel : ListingViewModel<CurrBorrowingDTO>() {

    private lateinit var address: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(vararg params: Any): List<CurrBorrowingDTO> {
        // TODO 对接后台接口
        delay(2000)
        return fakeData()
    }

    private fun fakeData(): List<CurrBorrowingDTO> {
        return mutableListOf(
            CurrBorrowingDTO(
                "VLSUSD",
                "VLSUSD",
                "00000000000000000000000000000001",
                "",
                "1001110000"
            ),
            CurrBorrowingDTO(
                "VLSEUR",
                "VLSEUR",
                "00000000000000000000000000000001",
                "",
                "1231410000"
            )
        )
    }

}
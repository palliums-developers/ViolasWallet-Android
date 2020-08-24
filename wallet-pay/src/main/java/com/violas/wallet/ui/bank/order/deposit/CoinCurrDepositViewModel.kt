package com.violas.wallet.ui.bank.order.deposit

import com.palliums.listing.ListingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.http.bank.CoinCurrDepositDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 16:32.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class CoinCurrDepositViewModel : ListingViewModel<CoinCurrDepositDTO>() {

    private lateinit var address: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(vararg params: Any): List<CoinCurrDepositDTO> {
        // TODO 对接后台接口
        delay(2000)
        //return fakeData()
        return emptyList()
    }

    private fun fakeData(): List<CoinCurrDepositDTO> {
        return mutableListOf(
            CoinCurrDepositDTO(
                "VLSUSD",
                "VLSUSD",
                "00000000000000000000000000000001",
                "",
                "1001110000",
                "23400000",
                "5.123"
            ),
            CoinCurrDepositDTO(
                "VLSEUR",
                "VLSEUR",
                "00000000000000000000000000000001",
                "",
                "1231410000",
                "33500000",
                "5.346"
            )
        )
    }
}
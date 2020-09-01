package com.violas.wallet.ui.bank.order.deposit

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.AccountDepositInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 16:32.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankDepositOrderViewModel : PagingViewModel<AccountDepositInfoDTO>() {

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
        onSuccess: (List<AccountDepositInfoDTO>, Any?) -> Unit
    ) {
        val list = bankService.getAccountDepositInfos(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }

    private fun fakeData(): List<AccountDepositInfoDTO> {
        return mutableListOf(
            AccountDepositInfoDTO(
                "1",
                "VLSUSD",
                "",
                "5.123",
                "1001110000",
                "23400000",
                "1001110000",
                1,
                "VLSUSD",
                "VLSUSD",
                "00000000000000000000000000000000"
            ),
            AccountDepositInfoDTO(
                "2",
                "VLSEUR",
                "",
                "5.254",
                "2003450000",
                "12200000",
                "1001110000",
                1,
                "VLSEUR",
                "VLSEUR",
                "00000000000000000000000000000000"
            )
        )
    }
}
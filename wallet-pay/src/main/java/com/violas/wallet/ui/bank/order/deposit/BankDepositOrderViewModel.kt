package com.violas.wallet.ui.bank.order.deposit

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.DepositInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 16:32.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankDepositOrderViewModel : PagingViewModel<DepositInfoDTO>() {

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
        onSuccess: (List<DepositInfoDTO>, Any?) -> Unit
    ) {
        val list = bankService.getDepositInfos(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }
}
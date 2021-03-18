package com.violas.wallet.ui.bank.order.borrowing

import com.palliums.paging.PagingViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.BorrowingInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/24 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankBorrowingOrderViewModel : PagingViewModel<BorrowingInfoDTO>() {

    private lateinit var address: String

    private val bankService by lazy { DataRepository.getBankService() }

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager.getAccountByCoinNumber(getViolasCoinType().coinNumber())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<BorrowingInfoDTO>, Any?) -> Unit
    ) {
        val list = bankService.getBorrowingInfos(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }
}
package com.violas.wallet.ui.bank.details.borrowing

import com.palliums.paging.PagingViewModel
import com.violas.wallet.event.UpdateBankBorrowedAmountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.CoinBorrowingRecordDTO
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/9/3 17:08.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class CoinBorrowingRecordViewModel(
    private val productId: String,
    private val walletAddress: String
) : PagingViewModel<CoinBorrowingRecordDTO>() {

    private val bankService by lazy { DataRepository.getBankService() }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<CoinBorrowingRecordDTO>, Any?) -> Unit
    ) {
        val coinBorrowingInfo =
            bankService.getCoinBorrowingRecords(
                walletAddress,
                productId,
                pageSize,
                (pageNumber - 1) * pageSize
            )

        // 更新待还金额
        coinBorrowingInfo?.let {
            EventBus.getDefault()
                .post(UpdateBankBorrowedAmountEvent(it.productId, it.borrowedAmount))
        }

        val records = coinBorrowingInfo?.records ?: emptyList()
        onSuccess.invoke(records, null)
    }
}
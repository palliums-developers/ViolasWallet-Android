package com.violas.wallet.ui.bank.details.borrowing

import com.palliums.paging.PagingViewModel
import com.violas.wallet.event.UpdateBankBorrowedAmountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.CoinLiquidationRecordDTO
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/9/3 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class CoinLiquidationRecordViewModel(
    private val productId: String,
    private val walletAddress: String
) : PagingViewModel<CoinLiquidationRecordDTO>() {

    private val bankService by lazy { DataRepository.getBankService() }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<CoinLiquidationRecordDTO>, Any?) -> Unit
    ) {
        val coinLiquidationInfo =
            bankService.getCoinLiquidationRecords(
                walletAddress,
                productId,
                pageSize,
                (pageNumber - 1) * pageSize
            )

        // 更新待还金额
        coinLiquidationInfo?.let {
            EventBus.getDefault()
                .post(UpdateBankBorrowedAmountEvent(it.productId, it.borrowedAmount))
        }

        val records = coinLiquidationInfo?.records ?: emptyList()
        onSuccess.invoke(records, null)
    }
}
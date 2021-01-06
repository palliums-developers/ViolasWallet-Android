package com.violas.wallet.ui.bank.details.borrowing

import com.palliums.paging.PagingViewModel
import com.violas.wallet.event.UpdateBankBorrowedAmountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.CoinRepaymentRecordDTO
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2020/9/3 17:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class CoinRepaymentRecordViewModel(
    private val productId: String,
    private val walletAddress: String
) : PagingViewModel<CoinRepaymentRecordDTO>() {

    private val bankService by lazy { DataRepository.getBankService() }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<CoinRepaymentRecordDTO>, Any?) -> Unit
    ) {
        val coinRepaymentInfo =
            bankService.getCoinRepaymentRecords(
                walletAddress,
                productId,
                pageSize,
                (pageNumber - 1) * pageSize
            )

        // 更新待还金额
        coinRepaymentInfo?.let {
            EventBus.getDefault().post(UpdateBankBorrowedAmountEvent(it.borrowedAmount))
        }

        val records = coinRepaymentInfo?.records ?: emptyList()
        onSuccess.invoke(records, null)
    }
}
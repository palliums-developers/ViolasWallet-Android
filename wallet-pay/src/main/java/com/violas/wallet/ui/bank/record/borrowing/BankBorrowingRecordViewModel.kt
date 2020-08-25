package com.violas.wallet.ui.bank.record.borrowing

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.http.bank.BorrowingRecordDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 2020/8/25 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankBorrowingRecordViewModel : PagingViewModel<BorrowingRecordDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<BorrowingRecordDTO>, Any?) -> Unit
    ) {
        // TODO 对接后台接口
        delay(2000)
        onSuccess.invoke(fakeData(), null)
    }

    private fun fakeData(): List<BorrowingRecordDTO> {
        return mutableListOf(
            BorrowingRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                0
            ),
            BorrowingRecordDTO(
                "VLSEUR",
                "VLSEUR",
                "1231410000",
                System.currentTimeMillis(),
                1
            ),
            BorrowingRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                2
            ),
            BorrowingRecordDTO(
                "VLSEUR",
                "VLSEUR",
                "1231410000",
                System.currentTimeMillis(),
                3
            ),
            BorrowingRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                4
            )
        )
    }
}
package com.violas.wallet.ui.bank.record.deposit

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.http.bank.DepositRecordDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 2020/8/25 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankDepositRecordViewModel : PagingViewModel<DepositRecordDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DepositRecordDTO>, Any?) -> Unit
    ) {
        // TODO 对接后台接口
        delay(2000)
        onSuccess.invoke(fakeData(), null)
    }

    private fun fakeData(): List<DepositRecordDTO> {
        return mutableListOf(
            DepositRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                0
            ),
            DepositRecordDTO(
                "VLSEUR",
                "VLSEUR",
                "1231410000",
                System.currentTimeMillis(),
                1
            ),
            DepositRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                2
            ),
            DepositRecordDTO(
                "VLSEUR",
                "VLSEUR",
                "1231410000",
                System.currentTimeMillis(),
                3
            ),
            DepositRecordDTO(
                "VLSUSD",
                "VLSUSD",
                "1001110000",
                System.currentTimeMillis(),
                4
            ),
            DepositRecordDTO(
                "VLSEUR",
                "VLSEUR",
                "1231410000",
                System.currentTimeMillis(),
                5
            )
        )
    }
}
package com.violas.wallet.ui.incentivePlan.miningEarnings

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.http.incentive.BankMiningEarningDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 11/27/20 11:25 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankMiningEarningsViewModel(
    private val walletAddress: String
) : PagingViewModel<BankMiningEarningDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<BankMiningEarningDTO>, Any?) -> Unit
    ) {
        // TODO 对接接口
        onSuccess.invoke(fakeData(), null)
    }

    private suspend fun fakeData(): List<BankMiningEarningDTO> {
        delay(500)
        return mutableListOf(
            BankMiningEarningDTO(
                System.currentTimeMillis(),
                1000_120000L,
                0
            ),
            BankMiningEarningDTO(
                System.currentTimeMillis(),
                2000_000000L,
                1
            ),
            BankMiningEarningDTO(
                System.currentTimeMillis(),
                2000_500000L,
                1
            )
        )
    }
}
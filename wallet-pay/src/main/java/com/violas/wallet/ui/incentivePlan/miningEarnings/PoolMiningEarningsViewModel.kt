package com.violas.wallet.ui.incentivePlan.miningEarnings

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.http.incentive.PoolMiningEarningDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 11/27/20 11:25 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class PoolMiningEarningsViewModel(
    private val walletAddress: String
) : PagingViewModel<PoolMiningEarningDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<PoolMiningEarningDTO>, Any?) -> Unit
    ) {
        // TODO 对接接口
        onSuccess.invoke(fakeData(), null)
    }

    private suspend fun fakeData(): List<PoolMiningEarningDTO> {
        delay(500)
        return mutableListOf(
            PoolMiningEarningDTO(
                System.currentTimeMillis(),
                1000_120000L,
                0
            ),
            PoolMiningEarningDTO(
                System.currentTimeMillis(),
                2000_000000L,
                1
            ),
            PoolMiningEarningDTO(
                System.currentTimeMillis(),
                2000_500000L,
                1
            )
        )
    }
}
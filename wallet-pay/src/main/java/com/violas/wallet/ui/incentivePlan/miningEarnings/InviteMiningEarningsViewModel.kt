package com.violas.wallet.ui.incentivePlan.miningEarnings

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.incentive.InviteMiningEarningDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 11/27/20 11:25 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class InviteMiningEarningsViewModel(
    private val walletAddress: String
) : PagingViewModel<InviteMiningEarningDTO>() {

    private val incentiveService by lazy {
        DataRepository.getIncentiveService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<InviteMiningEarningDTO>, Any?) -> Unit
    ) {
        val data = incentiveService.getInviteMiningEarnings(
            walletAddress,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(data, null)
    }

    private suspend fun fakeData(): List<InviteMiningEarningDTO> {
        delay(500)
        return mutableListOf(
            InviteMiningEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                1000_120000L,
                0
            ),
            InviteMiningEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                2000_000000L,
                1
            ),
            InviteMiningEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                2000_500000L,
                1
            )
        )
    }
}
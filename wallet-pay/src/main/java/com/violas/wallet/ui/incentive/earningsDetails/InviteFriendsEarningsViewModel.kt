package com.violas.wallet.ui.incentive.earningsDetails

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.incentive.InviteFriendsEarningDTO
import kotlinx.coroutines.delay

/**
 * Created by elephant on 11/27/20 11:25 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class InviteFriendsEarningsViewModel(
    private val walletAddress: String
) : PagingViewModel<InviteFriendsEarningDTO>() {

    private val incentiveService by lazy {
        DataRepository.getIncentiveService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<InviteFriendsEarningDTO>, Any?) -> Unit
    ) {
        val data = incentiveService.getInviteFriendsEarnings(
            walletAddress,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(data, null)
    }

    private suspend fun fakeData(): List<InviteFriendsEarningDTO> {
        delay(500)
        return mutableListOf(
            InviteFriendsEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                1000_120000L,
                0
            ),
            InviteFriendsEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                2000_000000L,
                1
            ),
            InviteFriendsEarningDTO(
                "2eiw8fjagalgl20wis9",
                System.currentTimeMillis(),
                2000_500000L,
                1
            )
        )
    }
}
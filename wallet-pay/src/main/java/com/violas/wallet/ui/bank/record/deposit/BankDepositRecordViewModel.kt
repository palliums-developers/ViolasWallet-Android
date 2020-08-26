package com.violas.wallet.ui.bank.record.deposit

import androidx.lifecycle.MutableLiveData
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

    // 当前的币种过滤器，Pair.first 表示所处列表位置，Pair.second 表示 coin name
    val currCoinFilterLiveData = MutableLiveData<Pair<Int, String?>>()

    // 当前的状态过滤器，Pair.first 表示所处列表位置，Pair.second 表示 state name
    val currStateFilterLiveData = MutableLiveData<Pair<Int, String?>>()

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
        val coinName = currCoinFilterLiveData.value?.second
        val state = currStateFilterLiveData.value?.first ?: 0

        return mutableListOf(
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 0 else state
            ),
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 1 else state
            ),
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 2 else state
            ),
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 3 else state
            ),
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 4 else state
            ),
            DepositRecordDTO(
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 5 else state
            )
        )
    }
}
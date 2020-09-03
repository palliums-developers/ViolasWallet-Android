package com.violas.wallet.ui.bank.record.deposit

import androidx.lifecycle.MutableLiveData
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bank.DepositRecordDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by elephant on 2020/8/25 16:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BankDepositRecordViewModel : PagingViewModel<DepositRecordDTO>() {

    // 当前的币种过滤器，Pair.first 表示所处列表位置，Pair.second 表示 coin name
    val currCoinFilterLiveData = MutableLiveData<Pair<Int, String>?>()

    // 当前的状态过滤器，Pair.first 表示所处列表位置，Pair.second 表示 state name
    val currStateFilterLiveData = MutableLiveData<Pair<Int, String>?>()

    // 币种过滤器数据
    val coinFilterDataLiveData = MutableLiveData<MutableList<String>>()

    // 状态过滤器数据
    val stateFilterDataLiveData = MutableLiveData<MutableList<String>>()

    private val bankService by lazy { DataRepository.getBankService() }

    private lateinit var address: String

    suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    suspend fun loadCoinFilterData() = withContext(Dispatchers.IO) {
        val depositProducts = bankService.getDepositProducts()
        val coinFilterData = depositProducts.map { it.tokenModule } as MutableList
        coinFilterData.add(0, getString(R.string.label_all))
        coinFilterDataLiveData.postValue(coinFilterData)
    }

    fun loadStateFilterData() {
        val stateFilterData = mutableListOf(
            getString(R.string.label_all),
            getString(R.string.bank_deposit_state_deposited),
            getString(R.string.bank_deposit_state_withdrew)
        )
        stateFilterDataLiveData.postValue(stateFilterData)
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<DepositRecordDTO>, Any?) -> Unit
    ) {
        val list = bankService.getDepositRecords(
            address,
            currCoinFilterLiveData.value?.second,
            currStateFilterLiveData.value?.first,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(list, null)
    }

    private fun fakeData(): List<DepositRecordDTO> {
        val coinName = currCoinFilterLiveData.value?.second
        val state = currStateFilterLiveData.value?.first ?: 0

        return mutableListOf(
            DepositRecordDTO(
                "1",
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 0 else state
            ),
            DepositRecordDTO(
                "2",
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 1 else state
            ),
            DepositRecordDTO(
                "3",
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 2 else state
            ),
            DepositRecordDTO(
                "4",
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 3 else state
            ),
            DepositRecordDTO(
                "5",
                if (coinName.isNullOrBlank()) "VLSUSD" else coinName,
                "",
                "1001110000",
                System.currentTimeMillis(),
                if (state == 0) 4 else state
            ),
            DepositRecordDTO(
                "6",
                if (coinName.isNullOrBlank()) "VLSEUR" else coinName,
                "",
                "1231410000",
                System.currentTimeMillis(),
                if (state == 0) 5 else state
            )
        )
    }
}
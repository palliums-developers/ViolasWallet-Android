package com.violas.wallet.ui.record

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.base.paging.PagingViewModel
import com.violas.wallet.repository.http.HttpInjector
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Created by elephant on 2019-11-07 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewModel
 */
class TransactionRecordViewModel(private val mAddress: String, coinTypes: CoinTypes) :
    PagingViewModel<TransactionRecordVO>() {

    private val mTransactionRepository =
        HttpInjector.getTransactionRepository(coinTypes)

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        mTransactionRepository.getTransactionRecord(
            mAddress, pageSize, pageNumber, pageKey, onSuccess, onFailure
        )

        //onSuccess.invoke(fakeData(mAddress, pageSize, pageNumber), null)
    }

    /**
     * code for test
     */
    private suspend fun fakeData(
        address: String,
        pageSize: Int,
        pageNumber: Int
    ): List<TransactionRecordVO> {
        delay(3000)

        val list = mutableListOf<TransactionRecordVO>()
        repeat(pageSize) {
            val id = (pageNumber - 1) * pageSize + it + 1

            val vo = TransactionRecordVO(
                id = id,
                coinTypes = when (it % 3) {
                    0 -> CoinTypes.Bitcoin
                    1 -> CoinTypes.Libra
                    else -> CoinTypes.VToken
                },
                transactionType = it % 2,
                time = System.currentTimeMillis(),
                amount = Random.nextLong(100000),
                address = "${address}00$id",
                url = "https://www.baidu.com/"
            )

            list.add(vo)
        }

        return list
    }
}
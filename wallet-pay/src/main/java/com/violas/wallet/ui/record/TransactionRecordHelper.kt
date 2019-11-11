package com.violas.wallet.ui.record

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.http.HttpInjector
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Created by elephant on 2019-11-07 19:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class TransactionRecordHelper(
    private val address: String,
    private val coinTypes: CoinTypes
) {

    suspend fun getTransactionRecord(
        pageSize: Int,
        pageIndex: Int,
        onSuccess: (List<TransactionRecordVO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {

        if (coinTypes == CoinTypes.Libra) {
            onSuccess.invoke(fakeData(pageSize, pageIndex, address, coinTypes), pageSize)
            return
        }

        if (coinTypes == CoinTypes.VToken) {
            onSuccess.invoke(fakeData(pageSize, pageIndex, address, coinTypes), pageSize)
            return
        }

        HttpInjector.bitcoinRepository
            .getTransactionRecord(address, pageSize, pageIndex + 1)
            .onSuccess {
                if (it.list.isNullOrEmpty()) {
                    onSuccess.invoke(emptyList(), 0)
                } else {

                    onSuccess.invoke(it.list!!.map { bean ->
                        TransactionRecordVO(
                            id = bean.block_height,
                            coinTypes = coinTypes,
                            transactionType = 1,
                            time = bean.block_time.toLong(),
                            amount = bean.outputs[0].value,
                            address = bean.outputs[0].addresses[0]
                        )
                    }, it.list!!.size)
                }
            }.onFailure {
                onFailure.invoke(it)
            }
    }
}

/**
 * code for test
 */
private suspend inline fun fakeData(
    pageSize: Int,
    pageIndex: Int,
    address: String,
    coinTypes: CoinTypes
): List<TransactionRecordVO> {
    delay(3000)

    val list = mutableListOf<TransactionRecordVO>()
    repeat(pageSize) {
        val vo = TransactionRecordVO(
            id = pageIndex + it + 1,
            coinTypes = coinTypes,
            transactionType = it % 2,
            time = System.currentTimeMillis(),
            amount = Random.nextLong(100000),
            address = "${address}000${pageIndex + it + 1}"
        )

        list.add(vo)
    }

    return list
}
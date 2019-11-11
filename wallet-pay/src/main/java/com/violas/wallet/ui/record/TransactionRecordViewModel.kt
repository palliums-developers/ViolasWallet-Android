package com.violas.wallet.ui.record

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.base.paging.PagingViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Created by elephant on 2019-11-07 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewModel
 */
class BitcoinTransactionRecordViewModel(private val helper: TransactionRecordHelper) :
    PagingViewModel<TransactionRecordVO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageIndex: Int,
        onSuccess: (List<TransactionRecordVO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        helper.getTransactionRecord(pageSize, pageIndex, onSuccess, onFailure)
    }
}

class LibraTransactionRecordViewModel(private val mAddress: String) :
    PagingViewModel<TransactionRecordVO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageIndex: Int,
        onSuccess: (List<TransactionRecordVO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口
        onSuccess.invoke(fakeData(pageSize, pageIndex), pageSize)
    }
}

class ViolasTransactionRecordViewModel(private val mAddress: String) :
    PagingViewModel<TransactionRecordVO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageIndex: Int,
        onSuccess: (List<TransactionRecordVO>, Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // TODO 对接接口
        onSuccess.invoke(fakeData(pageSize, pageIndex), pageSize)
    }
}

/**
 * code for test
 */
private suspend inline fun fakeData(pageSize: Int, pageIndex: Int): List<TransactionRecordVO> {
    delay(3000)

    val list = mutableListOf<TransactionRecordVO>()
    repeat(pageSize) {
        val vo = TransactionRecordVO(
            id = pageIndex + it + 1,
            coinTypes = when (it % 3) {
                0 -> CoinTypes.Bitcoin
                1 -> CoinTypes.Libra
                else -> CoinTypes.VToken
            },
            transactionType = it % 2,
            time = System.currentTimeMillis(),
            amount = Random.nextLong(100000),
            address = "mkYUsJ8N1AidNUySQGCpwswQUaoyL2Mu8L00000000000000${pageIndex + it + 1}"
        )

        list.add(vo)
    }

    return list
}
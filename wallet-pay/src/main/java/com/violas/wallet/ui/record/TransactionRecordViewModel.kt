package com.violas.wallet.ui.record

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.repository.DataRepository
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

/**
 * Created by elephant on 2019-11-07 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewModel
 */
class TransactionRecordViewModel(
    private val mAddress: String,
    private val mTokenIdx: Long?,
    private val mTokenName: String?,
    coinTypes: CoinTypes
) : PagingViewModel<TransactionRecordVO>() {

    private val mTransactionRepository =
        DataRepository.getTransactionService(coinTypes)

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        if (pageNumber == 1 && mTokenIdx != null) {
            // 在币种信息页面刷新时，通知Activity刷新余额
            EventBus.getDefault().post(RefreshBalanceEvent(0))
        }

        mTransactionRepository.getTransactionRecord(
            mAddress, mTokenIdx?.toString(), mTokenName, pageSize, pageNumber, pageKey, onSuccess
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
        delay(500)

        val list = mutableListOf<TransactionRecordVO>()
        repeat(pageSize) {
            val id = (pageNumber - 1) * pageSize + it + 1

            val vo = TransactionRecordVO(
                id = id,
                coinTypes = when (it % 3) {
                    0 -> CoinTypes.Bitcoin
                    1 -> CoinTypes.Libra
                    else -> CoinTypes.Violas
                },
                transactionType = it % 2,
                time = System.currentTimeMillis(),
                amount = Random.nextLong(100000).toString(),
                gas = "0",
                address = "${address}00$id",
                url = "https://www.baidu.com/"
            )

            list.add(vo)
        }

        return list
    }
}
package com.violas.wallet.ui.transactionRecord

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
    private val mWalletAddress: String,
    private val mTokenId: String?,
    private val mTokenDisplayName: String?,
    @TransactionType
    private val mTransactionType: Int,
    coinTypes: CoinTypes
) : PagingViewModel<TransactionRecordVO>() {

    private val mTransactionRecordService by lazy {
        DataRepository.getTransactionRecordService(coinTypes)
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        if (pageNumber == 1 && !mTokenId.isNullOrBlank()) {
            // 在币种信息页面刷新时，通知Activity刷新余额
            EventBus.getDefault().post(RefreshBalanceEvent(0))
        }

        mTransactionRecordService.getTransactionRecords(
            mWalletAddress,
            mTokenId,
            mTokenDisplayName,
            mTransactionType,
            pageSize,
            pageNumber,
            pageKey,
            onSuccess
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
                coinType = when (it % 3) {
                    0 -> CoinTypes.Bitcoin
                    1 -> CoinTypes.Libra
                    else -> CoinTypes.Violas
                },
                transactionType = when (it % 3) {
                    0 -> TransactionType.TRANSFER
                    1 -> TransactionType.COLLECTION
                    else -> TransactionType.ADD_CURRENCY
                },
                transactionState = when (it % 3) {
                    0 -> TransactionState.FAILURE
                    1 -> TransactionState.SUCCESS
                    else -> TransactionState.PENDING
                },
                time = System.currentTimeMillis(),
                fromAddress = "${address}00$id",
                toAddress = "${address}00$id",
                amount = Random.nextLong(100000).toString(),
                tokenId = mTokenId,
                tokenDisplayName = mTokenDisplayName,
                gas = "0",
                gasTokenId = mTokenId,
                gasTokenDisplayName = mTokenDisplayName,
                transactionId = it.toString(),
                url = "https://www.baidu.com/"
            )

            list.add(vo)
        }

        return list
    }
}
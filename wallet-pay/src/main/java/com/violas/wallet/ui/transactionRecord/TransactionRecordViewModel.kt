package com.violas.wallet.ui.transactionRecord

import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinType
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.repository.DataRepository
import org.greenrobot.eventbus.EventBus

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
    coinType: CoinType
) : PagingViewModel<TransactionRecordVO>() {

    private val mTransactionRecordService by lazy {
        DataRepository.getTransactionRecordService(coinType)
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
    }
}
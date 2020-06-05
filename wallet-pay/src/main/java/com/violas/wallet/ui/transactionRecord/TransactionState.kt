package com.violas.wallet.ui.transactionRecord

import androidx.annotation.IntDef

/**
 * Created by elephant on 2020/6/5 16:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易状态
 */
@IntDef(
    TransactionState.PENDING,
    TransactionState.FAILURE,
    TransactionState.SUCCESS
)
annotation class TransactionState {
    companion object {
        const val PENDING = -1          // 待定（处理中/等待中）
        const val FAILURE = 0           // 失败
        const val SUCCESS = 1           // 成功
    }
}
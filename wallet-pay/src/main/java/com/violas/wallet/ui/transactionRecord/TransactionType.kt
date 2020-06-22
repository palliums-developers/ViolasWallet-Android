package com.violas.wallet.ui.transactionRecord

import androidx.annotation.IntDef

/**
 * Created by elephant on 2020/6/3 17:55.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易类型
 */
@IntDef(
    TransactionType.ALL,
    TransactionType.TRANSFER,
    TransactionType.COLLECTION,
    TransactionType.ADD_CURRENCY,
    TransactionType.OTHER
)
annotation class TransactionType {
    companion object {
        const val ALL = 1               // 全部
        const val TRANSFER = 2          // 转账
        const val COLLECTION = 3        // 收款
        const val ADD_CURRENCY = 4      // 添加币种
        const val OTHER = 5             // 其它交易
    }
}
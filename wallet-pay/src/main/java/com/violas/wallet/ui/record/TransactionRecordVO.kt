package com.violas.wallet.ui.record

import com.quincysx.crypto.CoinTypes

/**
 * Created by elephant on 2019-11-07 11:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewObject
 */
data class TransactionRecordVO(
    val id: Int,
    val coinTypes: CoinTypes,
    val transactionType: Int,
    val time: Long,
    val amount: String,
    val address: String,
    val url: String? = null
) {
    companion object {
        /**
         * 加以类型：收款
         */
        const val TRANSACTION_TYPE_RECEIPT = 1
        /**
         * 加以类型：付款
         */
        const val TRANSACTION_TYPE_PAYMENT = 2

        fun isReceipt(transactionType: Int): Boolean {
            return transactionType == TRANSACTION_TYPE_RECEIPT
        }
    }
}
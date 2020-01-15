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
    val gas: String,
    val address: String,
    val url: String? = null,
    val coinName: String? = null
) {
    companion object {
        /**
         * 交易类型：收款
         */
        const val TRANSACTION_TYPE_RECEIPT = 1
        /**
         * 交易类型：转账
         */
        const val TRANSACTION_TYPE_TRANSFER = 2

        /**
         * 交易类型：token收款
         */
        const val TRANSACTION_TYPE_TOKEN_RECEIPT = 3
        /**
         * 交易类型：token转账
         */
        const val TRANSACTION_TYPE_TOKEN_TRANSFER = 4

        /**
         * 交易类型：开启token
         */
        const val TRANSACTION_TYPE_OPEN_TOKEN = 5

        fun isReceipt(transactionType: Int): Boolean {
            return transactionType == TRANSACTION_TYPE_RECEIPT ||
                    transactionType == TRANSACTION_TYPE_TOKEN_RECEIPT
        }

        fun isOpenToken(transactionType: Int): Boolean {
            return transactionType == TRANSACTION_TYPE_OPEN_TOKEN
        }

        fun isTokenTransaction(transactionType: Int): Boolean {
            return transactionType == TRANSACTION_TYPE_TOKEN_RECEIPT ||
                    transactionType == TRANSACTION_TYPE_TOKEN_TRANSFER
        }

        fun isTokenOpt(transactionType: Int): Boolean {
            return isOpenToken(transactionType) || isTokenTransaction(transactionType)
        }
    }
}
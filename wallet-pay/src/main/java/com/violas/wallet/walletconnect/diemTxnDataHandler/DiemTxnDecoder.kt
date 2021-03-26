package com.violas.wallet.walletconnect.diemTxnDataHandler

import com.violas.wallet.walletconnect.TransactionDataType

interface DiemTxnDecoder {

    fun isHandle(): Boolean

    fun getTransactionDataType(): TransactionDataType

    fun handle(): Any
}
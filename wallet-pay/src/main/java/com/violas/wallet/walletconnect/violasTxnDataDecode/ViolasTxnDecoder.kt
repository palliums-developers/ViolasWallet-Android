package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.violas.wallet.walletconnect.TransactionDataType

interface ViolasTxnDecoder {
    fun isHandle(): Boolean

    fun getTransactionDataType(): TransactionDataType

    fun handle(): Any
}
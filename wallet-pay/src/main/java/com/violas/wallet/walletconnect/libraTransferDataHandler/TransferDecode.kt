package com.violas.wallet.walletconnect.libraTransferDataHandler

import com.violas.wallet.walletconnect.walletConnectMessageHandler.TransactionDataType

interface TransferDecode {
    fun isHandle(): Boolean

    fun getTransactionDataType(): TransactionDataType

    fun handle(): Any
}
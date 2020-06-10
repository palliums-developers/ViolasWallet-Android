package com.violas.wallet.walletconnect.transferDataHandler

import com.violas.wallet.walletconnect.WalletConnect

interface TransferDecode {
    fun isHandle(): Boolean

    fun getTransactionDataType(): WalletConnect.TransactionDataType

    fun handle(): Any
}
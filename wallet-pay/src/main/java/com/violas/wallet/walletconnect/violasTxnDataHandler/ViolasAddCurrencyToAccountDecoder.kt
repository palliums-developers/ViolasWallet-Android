package com.violas.wallet.walletconnect.violasTxnDataHandler

import com.palliums.content.ContextProvider
import com.violas.wallet.walletconnect.PublishDataType
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.libracore.move.Move
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasAddCurrencyToAccountDecoder(private val transaction: RawTransaction) :
    ViolasTxnDecoder {

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload
        return payload is TransactionPayload.Script && payload.code.contentEquals(
            Move.decode(
                ContextProvider.getContext().assets.open("move/violas_add_currency_to_account.mv")
            )
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.ADD_CURRENCY_TO_ACCOUNT
    }

    override fun handle(): PublishDataType {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        val coinName = decodeCoinName(
            0,
            payload
        )
        return PublishDataType(
            transaction.sender.toHex(),
            coinName
        )
    }
}
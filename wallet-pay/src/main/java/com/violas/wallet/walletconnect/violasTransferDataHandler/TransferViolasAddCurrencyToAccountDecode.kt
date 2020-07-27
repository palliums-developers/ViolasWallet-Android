package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.content.ContextProvider
import com.violas.wallet.walletconnect.walletConnectMessageHandler.PublishDataType
import com.violas.wallet.walletconnect.walletConnectMessageHandler.TransactionDataType
import org.palliums.libracore.move.Move
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class TransferViolasAddCurrencyToAccountDecode(private val transaction: RawTransaction) :
    TransferDecode {

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload
        return payload is TransactionPayload.Script && payload.code.contentEquals(
            Move.decode(
                ContextProvider.getContext().assets.open("move/violas_add_currency_to_account.mv")
            )
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.PUBLISH
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
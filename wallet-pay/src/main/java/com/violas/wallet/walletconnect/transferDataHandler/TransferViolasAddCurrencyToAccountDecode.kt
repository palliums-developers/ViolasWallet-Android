package com.violas.wallet.walletconnect.transferDataHandler

import com.palliums.content.ContextProvider
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.walletconnect.WalletConnect
import org.palliums.libracore.move.Move
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class TransferViolasAddCurrencyToAccountDecode(private val transaction: RawTransaction) : TransferDecode {

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload
        return payload is TransactionPayload.Script && payload.code.contentEquals(
            Move.decode(
                ContextProvider.getContext().assets.open("move/violas_add_currency_to_account.mv")
            )
        )
    }

    override fun getTransactionDataType(): WalletConnect.TransactionDataType {
        return WalletConnect.TransactionDataType.Transfer
    }

    override fun handle(): WalletConnect.PublishDataType {
        return WalletConnect.PublishDataType(
            transaction.sender.toHex()
        )
    }
}
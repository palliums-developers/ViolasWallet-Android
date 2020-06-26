package com.violas.wallet.walletconnect.transferDataHandler

import com.palliums.content.ContextProvider
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.walletconnect.WalletConnect
import org.palliums.libracore.move.Move
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class TransferP2PWithDataDecode(private val transaction: RawTransaction) : TransferDecode {

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload
        return payload is TransactionPayload.Script && payload.code.contentEquals(
            Move.decode(
                ContextProvider.getContext().assets.open("move/violas_peer_to_peer_with_metadata.mv")
            )
        )
    }

    override fun getTransactionDataType(): WalletConnect.TransactionDataType {
        return WalletConnect.TransactionDataType.Transfer
    }

    override fun handle(): WalletConnect.TransferDataType {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        val coinName = decodeCoinName(
            0,
            payload
        )

        val data = byteArrayOf()

        return WalletConnect.TransferDataType(
            transaction.sender.toHex(),
            payload.args[0].decodeToValue() as String,
            payload.args[1].decodeToValue() as Long,
            coinName,
            Base64.encode(data)
        )
    }
}
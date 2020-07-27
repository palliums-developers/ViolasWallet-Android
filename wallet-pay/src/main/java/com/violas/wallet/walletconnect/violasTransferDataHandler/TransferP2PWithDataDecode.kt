package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.content.ContextProvider
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.walletconnect.walletConnectMessageHandler.ProcessedRuntimeException
import com.violas.wallet.walletconnect.walletConnectMessageHandler.TransactionDataType
import com.violas.wallet.walletconnect.walletConnectMessageHandler.TransferDataType
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

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.Transfer
    }

    override fun handle(): TransferDataType {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        return try {
            val coinName = decodeCoinName(
                0,
                payload
            )

            val data = decodeWithData(2, payload)

            TransferDataType(
                transaction.sender.toHex(),
                payload.args[0].decodeToValue() as String,
                payload.args[1].decodeToValue() as Long,
                coinName,
                Base64.encode(data)
            )
        } catch (e: ProcessedRuntimeException) {
            throw ProcessedRuntimeException(
                "peer_to_peer_with_metadata contract parameter list(payee: Address,\n" +
                        "    amount: Number,\n" +
                        "    metadata: Bytes,\n" +
                        "    metadata_signature: Bytes)"
            )
        }
    }
}
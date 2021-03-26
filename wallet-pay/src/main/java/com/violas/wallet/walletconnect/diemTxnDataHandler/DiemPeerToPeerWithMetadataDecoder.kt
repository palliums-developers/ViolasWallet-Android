package com.violas.wallet.walletconnect.diemTxnDataHandler

import com.palliums.content.ContextProvider
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.walletconnect.messageHandler.ProcessedRuntimeException
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.TransferDataType
import org.palliums.libracore.move.Move
import org.palliums.libracore.transaction.RawTransaction
import org.palliums.libracore.transaction.TransactionPayload

class DiemPeerToPeerWithMetadataDecoder(private val transaction: RawTransaction) : DiemTxnDecoder {

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload
        return payload is TransactionPayload.Script && payload.code.contentEquals(
            Move.decode(
                ContextProvider.getContext().assets.open("move/libra_peer_to_peer_with_metadata.mv")
            )
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.PEER_TO_PEER_WITH_METADATA
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
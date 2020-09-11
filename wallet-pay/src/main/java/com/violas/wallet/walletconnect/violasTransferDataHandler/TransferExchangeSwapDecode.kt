package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.common.Vm
import com.violas.wallet.walletconnect.ExchangeSwapDataType
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandler.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class TransferExchangeSwapDecode(private val transaction: RawTransaction) :
    TransferDecode {
    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(Vm.TestNet)
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload


        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasExchangeContract.getTokenSwapContract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_EXCHANGE_SWAP
    }

    override fun handle(): ExchangeSwapDataType {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        return try {
            val minCoinName = decodeCoinName(
                0,
                payload
            )

            val maxCoinName = decodeCoinName(
                1,
                payload
            )
            val path = (payload.args[3].decodeToValue() as ByteArray).map { it.toInt() }

            var fromCoinName: String = ""
            var toCoinName: String = ""
            if (path.size > 2) {
                if (path.first() > path.last()) {
                    fromCoinName = maxCoinName
                    toCoinName = minCoinName
                } else {
                    fromCoinName = minCoinName
                    toCoinName = maxCoinName
                }
            }

            val data = decodeWithData(4, payload)

            ExchangeSwapDataType(
                transaction.sender.toHex(),
                payload.args[0].decodeToValue() as String,
                fromCoinName,
                toCoinName,
                payload.args[1].decodeToValue() as Long,
                payload.args[2].decodeToValue() as Long,
                path,
                Base64.encode(data)
            )
        } catch (e: ProcessedRuntimeException) {
            throw ProcessedRuntimeException(
                "exchange_swap contract parameter list(payee: Address,\n" +
                        "    amount_in: U64,\n" +
                        "    amount_out_min: U64,\n" +
                        "    path: vector,\n" +
                        "    data: vector)"
            )
        }

    }
}
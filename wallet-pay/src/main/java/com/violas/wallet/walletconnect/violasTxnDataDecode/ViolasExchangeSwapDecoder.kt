package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.ExchangeSwapData
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandle.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasExchangeSwapDecoder(
    private val transaction: RawTransaction
) : ViolasTxnDecoder {

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
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

    override fun handle(): ExchangeSwapData {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        return try {
            val currencyA = decodeCurrencyCode(0, payload)
            val currencyB = decodeCurrencyCode(1, payload)
            val path = (payload.args[3].decodeToValue() as ByteArray).map { it.toInt() }
            val data = decodeWithData(4, payload)

            var currencyIn = ""
            var currencyOut = ""
            if (path.size >= 2) {
                if (path.first() > path.last()) {
                    currencyIn = currencyB
                    currencyOut = currencyA
                } else {
                    currencyIn = currencyA
                    currencyOut = currencyB
                }
            }

            ExchangeSwapData(
                transaction.sender.toHex(),
                payload.args[0].decodeToValue() as String,
                currencyIn,
                currencyOut,
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
package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.ExchangeAddLiquidityData
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandle.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasExchangeAddLiquidityDecoder(
    private val transaction: RawTransaction
) : ViolasTxnDecoder {

    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload

        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasExchangeContract.getAddLiquidityContract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_EXCHANGE_ADD_LIQUIDITY
    }

    override fun handle(): ExchangeAddLiquidityData {
        val payload = transaction.payload?.payload as TransactionPayload.Script

        return try {
            ExchangeAddLiquidityData(
                transaction.sender.toHex(),
                decodeCurrencyCode(0, payload),
                decodeCurrencyCode(1, payload),
                payload.args[0].decodeToValue() as Long,
                payload.args[1].decodeToValue() as Long,
                payload.args[2].decodeToValue() as Long,
                payload.args[3].decodeToValue() as Long
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
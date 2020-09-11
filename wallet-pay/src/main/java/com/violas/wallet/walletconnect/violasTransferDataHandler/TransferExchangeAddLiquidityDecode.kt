package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.common.Vm
import com.violas.wallet.walletconnect.ExchangeAddLiquidityDataType
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandler.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class TransferExchangeAddLiquidityDecode(private val transaction: RawTransaction) :
    TransferDecode {
    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(Vm.TestNet)
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

    override fun handle(): ExchangeAddLiquidityDataType {
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

            ExchangeAddLiquidityDataType(
                transaction.sender.toHex(),
                minCoinName,
                maxCoinName,
                payload.args[0].decodeToValue() as Long,
                payload.args[2].decodeToValue() as Long,
                payload.args[1].decodeToValue() as Long,
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
package com.violas.wallet.walletconnect.violasTxnDataHandler

import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.BankDepositDatatype
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandler.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasBankDepositDecoder(private val transaction: RawTransaction) : ViolasTxnDecoder {
    private val mViolasBankContract by lazy {
        ViolasBankContract(isViolasTestNet())
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload

        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasBankContract.getLock2Contract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_BANK_DEPOSIT
    }

    override fun handle(): BankDepositDatatype {
        val payload = transaction.payload?.payload as TransactionPayload.Script
        return try {
            val coinName = decodeCoinName(
                0,
                payload
            )

            BankDepositDatatype(
                transaction.sender.toHex(),
                payload.args[0].decodeToValue() as Long,
                coinName
            )
        } catch (e: ProcessedRuntimeException) {
            throw ProcessedRuntimeException(
                "bank_deposit contract parameter list(amount: Number,\n" +
                        "    metadata: Bytes)"
            )
        }
    }
}
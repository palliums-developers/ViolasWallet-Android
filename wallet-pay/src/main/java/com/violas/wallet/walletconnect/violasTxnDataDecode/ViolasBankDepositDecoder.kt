package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.BankDepositData
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandle.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasBankDepositDecoder(
    private val transaction: RawTransaction
) : ViolasTxnDecoder {

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

    override fun handle(): BankDepositData {
        val payload = transaction.payload?.payload as TransactionPayload.Script

        return try {
            BankDepositData(
                transaction.sender.toHex(),
                decodeCurrencyCode(0, payload),
                payload.args[0].decodeToValue() as Long
            )
        } catch (e: ProcessedRuntimeException) {
            throw ProcessedRuntimeException(
                "bank_deposit contract parameter list(amount: Number,\n" +
                        "    metadata: Bytes)"
            )
        }
    }
}
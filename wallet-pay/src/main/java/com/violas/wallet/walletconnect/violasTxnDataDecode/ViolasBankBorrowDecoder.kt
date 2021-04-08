package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.BankBorrowData
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.messageHandle.ProcessedRuntimeException
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

class ViolasBankBorrowDecoder(
    private val transaction: RawTransaction
) : ViolasTxnDecoder {

    private val mViolasBankContract by lazy {
        ViolasBankContract(isViolasTestNet())
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload

        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasBankContract.getBorrow2Contract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_BANK_BORROW
    }

    override fun handle(): BankBorrowData {
        val payload = transaction.payload?.payload as TransactionPayload.Script

        return try {
            BankBorrowData(
                transaction.sender.toHex(),
                decodeCurrencyCode(0, payload),
                payload.args[0].decodeToValue() as Long
            )
        } catch (e: ProcessedRuntimeException) {
            throw ProcessedRuntimeException(
                "bank_borrow contract parameter list(amount: Number,\n" +
                        "    metadata: Bytes)"
            )
        }
    }
}
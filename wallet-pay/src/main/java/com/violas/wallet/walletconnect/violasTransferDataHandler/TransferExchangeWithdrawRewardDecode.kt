package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.violas.smartcontract.ViolasExchangeContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

/**
 * 资金池提取激励奖励解码器
 */
class TransferExchangeWithdrawRewardDecode(private val transaction: RawTransaction) :
    TransferDecode {
    private val mViolasExchangeContract by lazy {
        ViolasExchangeContract(isViolasTestNet())
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload

        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasExchangeContract.getWithdrawRewardContract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_EXCHANGE_WITHDRAW_REWARD
    }

    override fun handle(): Any {
        return Any()
    }
}
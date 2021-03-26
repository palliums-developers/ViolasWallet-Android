package com.violas.wallet.walletconnect.violasTxnDataHandler

import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

/**
 * 数字银行提取激励奖励解码器
 */
class ViolasBankWithdrawRewardDecoder(private val transaction: RawTransaction) : ViolasTxnDecoder {
    private val mViolasBankContract by lazy {
        ViolasBankContract(isViolasTestNet())
    }

    override fun isHandle(): Boolean {
        val payload = transaction.payload?.payload

        return payload is TransactionPayload.Script && payload.code.contentEquals(
            mViolasBankContract.getClaimIncentiveContract()
        )
    }

    override fun getTransactionDataType(): TransactionDataType {
        return TransactionDataType.VIOLAS_BANK_WITHDRAW_REWARD
    }

    override fun handle(): Any {
        return Any()
    }
}
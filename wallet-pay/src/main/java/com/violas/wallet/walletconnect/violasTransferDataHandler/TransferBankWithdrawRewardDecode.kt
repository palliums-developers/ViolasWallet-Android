package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.Vm
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload

/**
 * 数字银行提取激励奖励解码器
 */
class TransferBankWithdrawRewardDecode(private val transaction: RawTransaction) : TransferDecode {
    private val mViolasBankContract by lazy {
        ViolasBankContract(Vm.TestNet)
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
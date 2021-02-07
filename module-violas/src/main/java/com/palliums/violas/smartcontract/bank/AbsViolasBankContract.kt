package com.palliums.violas.smartcontract.bank

import com.palliums.content.ContextProvider
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTag

abstract class AbsViolasBankContract {

    abstract fun getContractAddress(): String

    private fun getContractDefaultAddress() = "00000000000000000000000000000001".hexToBytes()

    private fun replaceContractAddress(contract: ByteArray): ByteArray {
        return Move.violasReplaceAddress(
            contract,
            getContractAddress().hexToBytes(),
            getContractDefaultAddress()
        )
    }

    fun getLock2Contract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/bank_lock2.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getRedeem2Contract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/bank_redeem2.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getBorrow2Contract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/bank_borrow2.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getRepayBorrow2Contract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/bank_repay_borrow2.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getClaimIncentiveContract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/bank_claim_incentive.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    /**
     * 往银行存钱
     * @param token 要锁定的币种
     * @param amount 借的金额
     * @param data
     */
    fun optionLockTransactionPayload(
        token: TypeTag,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ): TransactionPayload {

        return TransactionPayload(
            TransactionPayload.Script(
                getLock2Contract(),
                arrayListOf(token),
                arrayListOf(
                    TransactionArgument.newU64(amount),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }

    /**
     * 在银行取钱
     * @param token 要赎回的币种
     * @param amount 借的金额
     * @param data
     */
    fun optionRedeemTransactionPayload(
        token: TypeTag,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ): TransactionPayload {

        return TransactionPayload(
            TransactionPayload.Script(
                getRedeem2Contract(),
                arrayListOf(token),
                arrayListOf(
                    TransactionArgument.newU64(amount),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }

    /**
     * 向银行借钱
     * @param token 要借的币种
     * @param amount 借的金额
     * @param data
     */
    fun optionBorrowTransactionPayload(
        token: TypeTag,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ): TransactionPayload {

        return TransactionPayload(
            TransactionPayload.Script(
                getBorrow2Contract(),
                arrayListOf(token),
                arrayListOf(
                    TransactionArgument.newU64(amount),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }

    /**
     * 偿还银行贷款
     * @param token 要还款的币种
     * @param amount 借的金额
     * @param data
     */
    fun optionRepayBorrowTransactionPayload(
        token: TypeTag,
        amount: Long,
        data: ByteArray = byteArrayOf()
    ): TransactionPayload {

        return TransactionPayload(
            TransactionPayload.Script(
                getRepayBorrow2Contract(),
                arrayListOf(token),
                arrayListOf(
                    TransactionArgument.newU64(amount),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }

    /**
     * 提取挖矿奖励
     */
    fun optionWithdrawRewardTransactionPayload(): TransactionPayload {

        return TransactionPayload(
            TransactionPayload.Script(
                getClaimIncentiveContract(),
                arrayListOf(),
                arrayListOf()
            )
        )
    }
}
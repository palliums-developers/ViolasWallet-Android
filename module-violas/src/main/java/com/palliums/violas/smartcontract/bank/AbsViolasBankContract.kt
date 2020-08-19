package com.palliums.violas.smartcontract.bank

import com.palliums.violas.smartcontract.violasExchange.AbsViolasExchangeContract
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTag

abstract class AbsViolasBankContract {
    companion object {

        private const val mBorrow2Contract =
            "a11ceb0b010000000601000203020c040e0405120e07201c083c1000000001000101010002020101010003010303060c030a020002060c030109000a56696f6c617342616e6b06626f72726f7709657869745f62616e6b0000000000000000000000000000000101010001080a000a010b0238000b000a01380102"
        private const val mLock2Contract =
            "a11ceb0b010000000601000203020c040e0405120e07201b083b1000000001000101010002020101010003010302060c030003060c030a020109000a56696f6c617342616e6b0a656e7465725f62616e6b046c6f636b0000000000000000000000000000000101010201080a000a0138000b000a010b02380102"
        private const val mRedeem2Contract =
            "a11ceb0b010000000601000203020c040e0405120e07201c083c1000000001000101010002020101010103000302060c030003060c030a020109000a56696f6c617342616e6b09657869745f62616e6b0672656465656d0000000000000000000000000000000101010201080a000a010b0238000b000a01380102"
        private const val mRepayBorrow2Contract =
            "a11ceb0b010000000601000203020c040e0405120e07202308431000000001000101010002020101010003010302060c030003060c030a020109000a56696f6c617342616e6b0a656e7465725f62616e6b0c72657061795f626f72726f770000000000000000000000000000000101010201080a000a0138000b000a010b02380102"
    }

    abstract fun getContractAddress(): String

    private fun getContractDefaultAddress() = "00000000000000000000000000000001".hexToBytes()

    private fun replaceContractAddress(contract: String): ByteArray {
        return Move.violasReplaceAddress(
            contract.hexToBytes(),
            getContractAddress().hexToBytes(),
            getContractDefaultAddress()
        )
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
        val moveEncode = replaceContractAddress(mLock2Contract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
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
        val moveEncode = replaceContractAddress(mRedeem2Contract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
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
        val moveEncode = replaceContractAddress(mRepayBorrow2Contract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
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
        val moveEncode = replaceContractAddress(mBorrow2Contract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(token),
                arrayListOf(
                    TransactionArgument.newU64(amount),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }
}
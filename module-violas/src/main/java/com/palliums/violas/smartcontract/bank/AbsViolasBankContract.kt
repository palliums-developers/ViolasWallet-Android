package com.palliums.violas.smartcontract.bank

import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTag

abstract class AbsViolasBankContract {
    companion object {
        private const val mBorrow2Contract =
            "a11ceb0b0100000007010002030216041804051c180734310865100675050000000100010101000202010101000303040000040501000006010603060c030a020002060c0301060c010102060c0a020109000a56696f6c617342616e6b06626f72726f7709657869745f62616e6b0c69735f7075626c6973686564077075626c697368000000000000000000000000000000010a0202010001010001110a0011020921030605090a00070011030a000a010b0238000b000a01380102"
        private const val mLock2Contract =
            "a11ceb0b0100000007010002030216041804051c180734300864100674050000000100010101000202030000030401010100040501000006020602060c030001060c010103060c030a0202060c0a020109000a56696f6c617342616e6b0a656e7465725f62616e6b0c69735f7075626c6973686564046c6f636b077075626c697368000000000000000000000000000000010a0202010001010401110a0011010921030605090a00070011030a000a0138000b000a010b02380102"
        private const val mRedeem2Contract =
            "a11ceb0b0100000007010002030216041804051c1a0736310867100677050000000100010101000202030000030401000004050601010307000702060c030001060c010102060c0a0203060c030a0201030109000a56696f6c617342616e6b09657869745f62616e6b0c69735f7075626c6973686564077075626c6973680672656465656d000000000000000000000000000000010a0202010001010501120a0011010921030605090a00070011020a000a010b0238000c010b000a01380102"
        private const val mRepayBorrow2Contract =
            "a11ceb0b010000000701000203021c041e0605241a073e47088501100695010500000001000101010002020301010003000400000405030000050603010100070107040701060c010302060c0300010102060c0a0203060c030a020109000a56696f6c617342616e6b0e626f72726f775f62616c616e63650a656e7465725f62616e6b0c69735f7075626c6973686564077075626c6973680c72657061795f626f72726f77000000000000000000000000000000010a0202010001010603190a0011020921030605090a00070011030a0106000000000000000021030e05110a0038000c010a000a0138010b000a010b02380202"
        private const val mClaimIncentiveContract =
            "a11ceb0b010000000601000203020f05110b071c30084c10065c05000000010001000002000200000303010001060c00010102060c0a020a56696f6c617342616e6b0f636c61696d5f696e63656e746976650c69735f7075626c6973686564077075626c697368000000000000000000000000000000010a020201000000010c0a0011010921030605090a00070011020b00110002"
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

    fun getBorrow2Contract() = replaceContractAddress(mBorrow2Contract)
    fun getLock2Contract() = replaceContractAddress(mLock2Contract)
    fun getRedeem2Contract() = replaceContractAddress(mRedeem2Contract)
    fun getRepayBorrow2Contract() = replaceContractAddress(mRepayBorrow2Contract)
    fun getWithdrawRewardContract() = replaceContractAddress(mClaimIncentiveContract)

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

    /**
     * 提取挖矿奖励
     */
    fun optionWithdrawRewardTransactionPayload(): TransactionPayload {
        val moveEncode = getWithdrawRewardContract()

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf()
            )
        )
    }
}
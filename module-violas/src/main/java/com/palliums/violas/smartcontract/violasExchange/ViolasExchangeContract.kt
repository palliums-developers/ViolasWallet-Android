package com.palliums.violas.smartcontract.violasExchange

import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload

abstract class ViolasExchangeContract {
    companion object {

        private const val mAddLiquidityContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000007000000075200000023000000067500000010000000098500000016000000000000010102000100000300010005030303030300063c53454c463e0845786368616e67650d6164645f6c6971756964697479046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030007000a000a010a020a030a04120002"
        private const val mInitializeContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000001000000074c00000020000000066c00000010000000097c0000000c000000000000010102000000000300000000063c53454c463e0845786368616e67650a696e697469616c697a65046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff03000200120002"
        private const val mPublishContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000001000000074c0000001d00000006690000001000000009790000000c000000000000010102000000000300000000063c53454c463e0845786368616e6765077075626c697368046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff03000200120002"
        private const val mRemoveLiquidityContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000007000000075200000026000000067800000010000000098800000016000000000000010102000100000300010005030303030300063c53454c463e0845786368616e67651072656d6f76655f6c6971756964697479046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030007000a010a000a020a030a04120002"
        private const val mTokenToTokenSwapContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b0000000800000007530000002f00000006820000001000000009920000001800000000000001010200010000030001000603030303030300063c53454c463e0845786368616e676519746f6b656e5f746f5f746f6b656e5f737761705f696e707574046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030008000a010a000a030a020a040a05120002"
        private const val mTokenToViolasSwapContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b000000060000000751000000300000000681000000100000000991000000140000000000000101020001000003000100040303030300063c53454c463e0845786368616e67651a746f6b656e5f746f5f76696f6c61735f737761705f696e707574046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a010a000a020a03120002"
        private const val mTransferContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b0000000900000007540000001e0000000672000000100000000982000000120000000000000101020001000003020100030503030003030503063c53454c463e0845786368616e6765087472616e73666572046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030205000a010a000a02120002"
        private const val mViolasToTokenSwapContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b000000060000000751000000300000000681000000100000000991000000140000000000000101020001000003000100040303030300063c53454c463e0845786368616e67651a76696f6c61735f746f5f746f6b656e5f737761705f696e707574046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a010a000a020a03120002"
    }

    abstract fun getContractAddress(): String

    private fun getContractDefaultAddress() = "7257c2417e4d1038e1817c8f283ace2e".hexToBytes()

    /**
     * 增加流动性
     */
    fun optionAddLiquidityTransactionPayload(
        tokenIdx: Long, minLiquidity: Long, maxTokenAmount: Long, violasAmount: Long, deadline: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mAddLiquidityContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val minLiquidityArgument = TransactionArgument.newU64(minLiquidity)
        val maxTokenAmountArgument = TransactionArgument.newU64(maxTokenAmount)
        val violasAmountArgument = TransactionArgument.newU64(violasAmount)
        val deadlineArgument = TransactionArgument.newU64(deadline)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    tokenIdxArgument,
                    minLiquidityArgument,
                    maxTokenAmountArgument,
                    violasAmountArgument,
                    deadlineArgument
                )
            )
        )
    }

    /**
     * 删除流动性
     */
    fun optionRemoveLiquidityTransactionPayload(
        tokenIdx: Long, amount: Long, minViolas: Long, minTokens: Long, deadline: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mRemoveLiquidityContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val amountArgument = TransactionArgument.newU64(amount)
        val minViolasArgument = TransactionArgument.newU64(minViolas)
        val minTokensArgument = TransactionArgument.newU64(minTokens)
        val deadlineArgument = TransactionArgument.newU64(deadline)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    tokenIdxArgument,
                    amountArgument,
                    minViolasArgument,
                    minTokensArgument,
                    deadlineArgument
                )
            )
        )
    }

    fun optionInitializeTransactionPayload(): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mInitializeContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf()
            )
        )
    }

    fun optionPublishTransactionPayload(): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mPublishContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf()
            )
        )
    }

    fun optionTokenToTokenSwapTransactionPayload(
        tokenSoldIdx: Long,
        tokensSold: Long,
        tokenBoughtIdx: Long,
        minTokensBought: Long,
        minViolasBought: Long,
        deadline: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mTokenToTokenSwapContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val tokenSoldIdxArgument = TransactionArgument.newU64(tokenSoldIdx)
        val tokensSoldArgument = TransactionArgument.newU64(tokensSold)
        val tokenBoughtIdxArgument = TransactionArgument.newU64(tokenBoughtIdx)
        val minTokensBoughtArgument = TransactionArgument.newU64(minTokensBought)
        val minViolasBoughtArgument = TransactionArgument.newU64(minViolasBought)
        val deadlineArgument = TransactionArgument.newU64(deadline)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    tokenSoldIdxArgument,
                    tokensSoldArgument,
                    tokenBoughtIdxArgument,
                    minTokensBoughtArgument,
                    minViolasBoughtArgument,
                    deadlineArgument
                )
            )
        )
    }

    fun optionTokenToViolasSwapTransactionPayload(
        tokenIdx: Long, tokensSold: Long, minViolas: Long, deadline: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mTokenToViolasSwapContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val tokensSoldArgument = TransactionArgument.newU64(tokensSold)
        val minViolasArgument = TransactionArgument.newU64(minViolas)
        val deadlineArgument = TransactionArgument.newU64(deadline)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    tokenIdxArgument,
                    tokensSoldArgument,
                    minViolasArgument,
                    deadlineArgument
                )
            )
        )
    }

    fun optionViolasToTokenSwapTransactionPayload(
        tokenIdx: Long, violasSold: Long, minTokens: Long, deadline: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mViolasToTokenSwapContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val violasSoldArgument = TransactionArgument.newU64(violasSold)
        val minTokensArgument = TransactionArgument.newU64(minTokens)
        val deadlineArgument = TransactionArgument.newU64(deadline)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    tokenIdxArgument,
                    violasSoldArgument,
                    minTokensArgument,
                    deadlineArgument
                )
            )
        )
    }

    fun optionTransferTransactionPayload(
        index: Long, payee: String, amount: Long
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(
                mTransferContract.hexToBytes(),
                getContractAddress().hexToBytes(),
                getContractDefaultAddress()
            )

        val indexArgument = TransactionArgument.newU64(index)
        val payeeArgument = TransactionArgument.newAddress(payee)
        val amountArgument = TransactionArgument.newU64(amount)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                    indexArgument,
                    payeeArgument,
                    amountArgument
                )
            )
        )
    }
}

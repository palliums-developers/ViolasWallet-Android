package com.palliums.violas.smartcontract.violasExchange

import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTag

abstract class AbsViolasExchangeContract {
    companion object {

        private const val mAddLiquidityContract =
            "a11ceb0b010006010002030207040902050b0d071817082f10000000010001020101000205060c030303030002090009010845786368616e67650d6164645f6c69717569646974797257c2417e4d1038e1817c8f283ace2e0201010001070b000a010a020a030a04380002"
        private const val mInitializeContract =
            "a11ceb0b010005010002030205050704070b14081f100000000100010001060c000845786368616e67650a696e697469616c697a657257c2417e4d1038e1817c8f283ace2e000001030b00110002"
        private const val mRemoveLiquidityContract =
            "a11ceb0b010006010002030207040902050b0c07171a083110000000010001020101000204060c0303030002090009010845786368616e67651072656d6f76655f6c69717569646974797257c2417e4d1038e1817c8f283ace2e0201010001060b000a010a020a03380002"
        private const val mTokenSwapContract =
            "a11ceb0b010006010002030207040902050b10071b0e082910000000010001020101000206060c0503030a020a020002090009010845786368616e676504737761707257c2417e4d1038e1817c8f283ace2e0201010001080b000a010a020a030b040b05380002"
        private const val mAddCurrencyContract =
            "a11ceb0b010006010006030617041d0605230b072e46087420000001010102000300010101020400020001050203010101030001010100040204030401060c00010501010109000845786368616e67650c4c696272614163636f756e74065369676e65720c6164645f63757272656e63790a616464726573735f6f6610616363657074735f63757272656e63797257c2417e4d1038e1817c8f283ace2e00000000000000000000000000000001010100010e0a0038000a0011013801200308050b0b003802050d0b000102"
    }

    abstract fun getContractAddress(): String

    private fun getContractDefaultAddress() = "7257c2417e4d1038e1817c8f283ace2e".hexToBytes()

    private fun replaceContractAddress(contract: String): ByteArray {
        return Move.violasReplaceAddress(
            contract.hexToBytes(),
            getContractAddress().hexToBytes(),
            getContractDefaultAddress()
        )
    }

    /**
     * 增加流动性交易对
     * @param tokenA 准备充入的流动性 Token A
     * @param tokenB 准备充入的流动性 Token B
     * @param tokenAExpectedAmount 流动性 Token A 的预期充值数量
     * @param tokenBExpectedAmount 流动性 Token B 的预期充值数量
     * @param tokenAMinAmount 流动性 Token A 最小接受的充值数量
     * @param tokenBMinAmount 流动性 Token B 最小接受的充值数量
     */
    fun optionAddLiquidityTransactionPayload(
        tokenA: TypeTag,
        tokenB: TypeTag,
        tokenAExpectedAmount: Long,
        tokenBExpectedAmount: Long,
        tokenAMinAmount: Long,
        tokenBMinAmount: Long
    ): TransactionPayload {
        val moveEncode = replaceContractAddress(mAddLiquidityContract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(tokenA, tokenB),
                arrayListOf(
                    TransactionArgument.newU64(tokenAExpectedAmount),
                    TransactionArgument.newU64(tokenBExpectedAmount),
                    TransactionArgument.newU64(tokenAMinAmount),
                    TransactionArgument.newU64(tokenBMinAmount)
                )
            )
        )
    }

    /**
     * 删除流动性
     * @param tokenA 准备充入的流动性 Token A
     * @param tokenB 准备充入的流动性 Token B
     * @param liquidityAmount 取出的流动性 Token 数量
     * @param tokenAMinAmount 流动性 Token A 最小接受的充值数量
     * @param tokenBMinAmount 流动性 Token B 最小接受的充值数量
     */
    fun optionRemoveLiquidityTransactionPayload(
        tokenA: TypeTag,
        tokenB: TypeTag,
        liquidityAmount: Long,
        tokenAMinAmount: Long,
        tokenBMinAmount: Long
    ): TransactionPayload {
        val moveEncode = replaceContractAddress(mRemoveLiquidityContract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(tokenA, tokenB),
                arrayListOf(
                    TransactionArgument.newU64(liquidityAmount),
                    TransactionArgument.newU64(tokenAMinAmount),
                    TransactionArgument.newU64(tokenBMinAmount)
                )
            )
        )
    }

    /**
     *  发起稳定币兑换
     * @param tokenA 支付的 Token
     * @param tokenB 兑换的目标 Token
     * @param payeeAddress 兑换接收地址
     * @param tokenAInputAmount 支付 Token 的数量
     * @param tokenBOutputMinAmount 兑换的目标 Token，最低接受的兑换数量
     * @param path 币种兑换数量
     * @param data 附加数据
     */
    fun optionTokenSwapTransactionPayload(
        tokenA: TypeTag,
        tokenB: TypeTag,
        payeeAddress: String,
        tokenAInputAmount: Long,
        tokenBOutputMinAmount: Long,
        path: ByteArray,
        data: ByteArray
    ): TransactionPayload {
        val moveEncode = replaceContractAddress(mTokenSwapContract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(tokenA, tokenB),
                arrayListOf(
                    TransactionArgument.newAddress(payeeAddress),
                    TransactionArgument.newU64(tokenAInputAmount),
                    TransactionArgument.newU64(tokenBOutputMinAmount),
                    TransactionArgument.newByteArray(path),
                    TransactionArgument.newByteArray(data)
                )
            )
        )
    }

    fun optionInitializeTransactionPayload(): TransactionPayload {
        val moveEncode = replaceContractAddress(mInitializeContract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf(
                )
            )
        )
    }

    fun optionAddCurrencyTransactionPayload(
        token: TypeTag
    ): TransactionPayload {
        val moveEncode = replaceContractAddress(mAddCurrencyContract)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(token),
                arrayListOf()
            )
        )
    }

    fun getAddLiquidityContract() = replaceContractAddress(mAddLiquidityContract)
    fun getRemoveLiquidityContract() = replaceContractAddress(mRemoveLiquidityContract)
    fun getTokenSwapContract() = replaceContractAddress(mTokenSwapContract)
}

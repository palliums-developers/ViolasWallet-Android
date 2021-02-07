package com.palliums.violas.smartcontract.violasExchange

import com.palliums.content.ContextProvider
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.storage.TypeTag

abstract class AbsViolasExchangeContract {

    abstract fun getContractAddress(): String

    private fun getContractDefaultAddress() = "00000000000000000000000000000001".hexToBytes()

    private fun replaceContractAddress(contract: ByteArray): ByteArray {
        return Move.violasReplaceAddress(
            contract,
            getContractAddress().hexToBytes(),
            getContractDefaultAddress()
        )
    }

    fun getAddLiquidityContract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/dex_add_liquidity.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getRemoveLiquidityContract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/dex_remove_liquidity.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getTokenSwapContract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/dex_swap.mv"
            )
        )
        return replaceContractAddress(contract)
    }

    fun getWithdrawMineRewardContract(): ByteArray {
        val contract = Move.decode(
            ContextProvider.getContext().assets.open(
                "move/dex_withdraw_mine_reward.mv"
            )
        )
        return replaceContractAddress(contract)
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
        val moveEncode = getAddLiquidityContract()

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
        val moveEncode = getRemoveLiquidityContract()

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
        val moveEncode = getTokenSwapContract()

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

    /**
     * 提取挖矿奖励
     */
    fun optionWithdrawRewardTransactionPayload(): TransactionPayload {
        val moveEncode = getWithdrawMineRewardContract()

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(),
                arrayListOf()
            )
        )
    }
}

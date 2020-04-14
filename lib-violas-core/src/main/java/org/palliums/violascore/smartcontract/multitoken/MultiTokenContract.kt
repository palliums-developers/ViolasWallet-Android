package org.palliums.violascore.smartcontract.multitoken

import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.lbrStructTag

class MultiTokenContract(
    private val contractAddress: String,
    private val multiContractRpcApi: MultiContractRpcApi
) {
    companion object {
        private const val mTransferContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b000000070000000752000000210000000673000000100000000983000000140000000000000101020001000003000100040305030a0200063c53454c463e0b56696f6c6173546f6b656e087472616e73666572046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a000a010a020b03120002"

        private const val mPublishContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b00000004000000074f00000020000000066f00000010000000097f0000000e0000000000000101020001000003000100010a0200063c53454c463e0b56696f6c6173546f6b656e077075626c697368046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030003000b00120002"

        private const val mMintContract =
            "a11ceb0b010006013d0000000400000003410000000a000000054b0000000700000007520000001d000000066f00000010000000097f000000140000000000000101020001000003000100040305030a0200063c53454c463e0b56696f6c6173546f6b656e046d696e74046d61696e7257c2417e4d1038e1817c8f283ace2e010000ffff030006000a000a010a020b03120002"

    }

    fun getBalance(tokenIndex: Long): Long {
        return multiContractRpcApi.getBalance(tokenIndex)
    }

    /**
     * 创建 Token 转账 payload
     */
    fun optionTokenTransactionPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mTransferContract.hexToBytes(), contractAddress.hexToBytes())

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val addressArgument = TransactionArgument.newAddress(address)
        val amountArgument = TransactionArgument.newU64(amount)
        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf(tokenIdxArgument, addressArgument, amountArgument, dataArgument)
            )
        )
    }

    /**
     * 注册 Token 交易 payload
     */
    fun optionPublishTransactionPayload(
        data: ByteArray
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mPublishContract.hexToBytes(), contractAddress.hexToBytes())

        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf(dataArgument)
            )
        )
    }

    /**
     * 铸造 Token 交易 payload
     */
    fun optionMintTransactionPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        val moveEncode =
            Move.violasReplaceAddress(mMintContract.hexToBytes(), contractAddress.hexToBytes())

        val tokenIdxArgument = TransactionArgument.newU64(tokenIdx)
        val addressArgument = TransactionArgument.newAddress(address)
        val amountArgument = TransactionArgument.newU64(amount)
        val dataArgument = TransactionArgument.newByteArray(data)

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf(tokenIdxArgument, addressArgument, amountArgument, dataArgument)
            )
        )
    }
}
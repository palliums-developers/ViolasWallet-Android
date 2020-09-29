package com.palliums.violas.http

import com.palliums.net.await
import com.palliums.violas.smartcontract.multitoken.MultiContractRpcApi
import com.palliums.violas.smartcontract.multitoken.MultiTokenContract
import org.palliums.violascore.transaction.TransactionPayload

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasMultiTokenRepository(
    private val mMultiContractApi: MultiContractRpcApi,
    private val multiTokenContract: MultiTokenContract
) {

    fun getMultiTokenContract() = multiTokenContract

    suspend fun getBalance(
        address: String,
        tokenIdx: List<Long>
    ) =
        mMultiContractApi.getBalance(
            address, tokenIdx.joinToString(",")
        ).await().data

    suspend fun getSupportCurrency() =
        mMultiContractApi.getSupportCurrency().await().data?.currencies

    suspend fun getRegisterToken(address: String) =
        mMultiContractApi.getRegisterToken(address).await().data?.isPublished == 1

    fun publishTokenPayload(data: ByteArray = byteArrayOf()): TransactionPayload {
        return multiTokenContract.optionPublishTransactionPayload(data)
    }

    fun transferTokenPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        return multiTokenContract.optionTokenTransactionPayload(tokenIdx, address, amount, data)
    }

    fun mintTokenPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        return multiTokenContract.optionMintTransactionPayload(tokenIdx, address, amount, data)
    }
}


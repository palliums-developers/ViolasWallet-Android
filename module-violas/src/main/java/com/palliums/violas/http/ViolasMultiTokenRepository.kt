package com.palliums.violas.http

import com.palliums.net.checkResponse
import com.palliums.violas.smartcontract.multitoken.*
import com.palliums.violas.smartcontract.multitoken.BalanceDTO
import com.palliums.violas.smartcontract.multitoken.SupportCurrencyDTO
import io.reactivex.Single
import org.palliums.violascore.transaction.TransactionPayload
import retrofit2.Call
import java.lang.Exception
import java.lang.RuntimeException

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

    suspend fun getBalance(
        address: String,
        tokenIdx: List<Long>? = null
    ): BalanceDTO? {
        val tokenIdxArr: String? = tokenIdx?.joinToString(",")
        return checkResponse {
            if (tokenIdxArr == null) {
                mMultiContractApi.getBalance(address)
            } else {
                mMultiContractApi.getBalance(address, tokenIdxArr)
            }
        }.data
    }

    suspend fun getSupportCurrency(): List<SupportCurrencyDTO>? {
        return checkResponse { mMultiContractApi.getSupportCurrency() }.data?.currencies
    }

    suspend fun getRegisterToken(address: String) =
        checkResponse { mMultiContractApi.getRegisterToken(address) }?.data?.isPublished == 1

    fun publishTokenPayload(data: ByteArray = byteArrayOf()): TransactionPayload {
        return multiTokenContract.optionPublishTransactionPayload()
    }

    fun transferTokenPayload(
        tokenIdx: Long,
        address: String,
        amount: Long,
        data: ByteArray
    ): TransactionPayload {
        return multiTokenContract.optionTokenTransactionPayload(tokenIdx, address, amount, data)
    }
}


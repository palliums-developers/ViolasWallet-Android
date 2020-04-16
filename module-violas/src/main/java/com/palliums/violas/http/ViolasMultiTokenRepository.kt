package com.palliums.violas.http

import com.palliums.net.checkResponse
import com.palliums.violas.smartcontract.multitoken.*
import com.palliums.violas.smartcontract.multitoken.BalanceDTO
import com.palliums.violas.smartcontract.multitoken.SupportCurrencyDTO
import io.reactivex.Single
import org.palliums.violascore.transaction.TransactionPayload
import java.lang.Exception

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
    suspend fun getSupportToken() =
        checkResponse {
            mMultiContractApi.getSupportCurrency()
        }

    fun getBalance(
        address: String,
        tokenIdx: List<Long>? = null
    ): Single<Response<BalanceDTO>> {
        val tokenIdxArr: String? = tokenIdx?.joinToString(",")
        return if (tokenIdxArr == null) {
            mMultiContractApi.getBalance(address)
        } else {
            mMultiContractApi.getBalance(address, tokenIdxArr)
        }
    }

    fun getSupportCurrency(): List<SupportCurrencyDTO>? {
        try {
            val execute = mMultiContractApi.getSupportCurrency().execute()
            if (execute.isSuccessful) {
                return execute.body()?.data?.currencies
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun getRegisterToken(address: String) =
        mMultiContractApi.getRegisterToken(address)

    fun publishTokenPayload(data: ByteArray = byteArrayOf()): TransactionPayload {
        return multiTokenContract.optionPublishTransactionPayload()
    }
}


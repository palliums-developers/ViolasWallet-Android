package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasRepository(private val mViolasApi: ViolasApi) {

    @Throws(RequestException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        offset: Int,
        tokenAddress: String?
    ): ListResponse<TransactionRecordDTO> {
        return checkResponse {
            mViolasApi.getTransactionRecord(address, pageSize, offset, tokenAddress)
        }
    }

    suspend fun getSupportToken() =
        checkResponse {
            mViolasApi.getSupportToken()
        }

    /**
     * 登录网页端钱包
     */
    suspend fun loginWeb(
        loginType: Int,
        sessionId: String,
        walletAccounts: List<WalletAccountDTO>
    ) =
        checkResponse {
            val requestBody = Gson()
                .toJson(LoginWebDTO(loginType, sessionId, walletAccounts))
                .toRequestBody("application/json".toMediaTypeOrNull())
            mViolasApi.loginWeb(requestBody)
        }

    @Deprecated("准备移除稳定币相关的余额查询")
    fun getBalance(
        address: String,
        tokenAddressList: List<String>? = null
    ): Single<Response<BalanceDTO>> {
        val tokenAddressArr: String? = tokenAddressList?.joinToString(",")
        return mViolasApi.getBalance(address, tokenAddressArr)
    }

    fun getSequenceNumber(address: String) =
        mViolasApi.getSequenceNumber(address)

    @Throws()
    suspend fun pushTx(tx: String): Response<Any> {
        val requestBody = Gson().toJson(SignedTxnDTO(tx))
            .toRequestBody("application/json".toMediaTypeOrNull())
        return mViolasApi.pushTx(requestBody)
    }

    @Deprecated("准备迁移到 MultiTokenContract")
    fun getSupportCurrency() =
        mViolasApi.getSupportCurrency()

    @Deprecated("准备迁移到 MultiTokenContract")
    fun getRegisterToken(address: String) =
        mViolasApi.getRegisterToken(address)

}
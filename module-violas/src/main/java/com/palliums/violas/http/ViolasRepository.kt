package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import com.palliums.violas.error.ViolasException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasRepository(private val mViolasApi: ViolasApi) {

    /**
     * 获取平台币余额
     */
    @Throws(RequestException::class)
    suspend fun getBalance(walletAddress: String) =
        checkResponse {
            mViolasApi.getBalance(walletAddress)
        }.data?.balance ?: 0

    /**
     * 获取 sequence number
     */
    @Throws(RequestException::class)
    suspend fun getSequenceNumber(address: String) =
        checkResponse {
            mViolasApi.getSequenceNumber(address)
        }.data ?: 0

    @Throws(ViolasException::class)
    suspend fun pushTx(tx: String): Response<Any> {
        val requestBody = Gson().toJson(SignedTxnDTO(tx))
            .toRequestBody("application/json".toMediaTypeOrNull())
        val pushTx = mViolasApi.pushTx(requestBody)
        ViolasException.checkViolasTransactionException(pushTx)
        return pushTx
    }

    /**
     * 获取交易记录
     */
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

    /**
     * 获取账户信息
     */
    suspend fun getAccountState(address: String): Response<AccountStateDTO> {
        return checkResponse {
            mViolasApi.getAccountState(address)
        }
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
}
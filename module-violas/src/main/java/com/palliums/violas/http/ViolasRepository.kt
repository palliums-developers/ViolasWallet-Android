package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.exceptions.RequestException
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

    @Throws(ViolasException::class, RequestException::class)
    suspend fun pushTx(tx: String): Response<Any> {
        return checkResponse(
            checkError = {
                ViolasException.checkViolasTransactionException(it)
            }
        ) {
            val requestBody = Gson().toJson(SignedTxnDTO(tx))
                .toRequestBody("application/json".toMediaTypeOrNull())
            mViolasApi.pushTx(requestBody)
        }
    }

    /**
     * 获取交易记录
     */
    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        tokenId: String?,
        pageSize: Int,
        offset: Int,
        transactionType: Int?
    ) =
        checkResponse {
            mViolasApi.getTransactionRecords(address, tokenId, pageSize, offset, transactionType)
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
     * 激活账户
     */
    suspend fun activateAccount(address: String, authKeyPrefix: String): Response<Any> {
        return checkResponse {
            mViolasApi.activateAccount(address, authKeyPrefix)
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

    @Throws(RequestException::class)
    suspend fun getCurrencies() = checkResponse {
        mViolasApi.getCurrency()
    }

    @Throws(RequestException::class)
    suspend fun getBTCChainFiatBalance(address: String) = checkResponse {
        mViolasApi.getBTCChainFiatBalance(address)
    }

    @Throws(RequestException::class)
    suspend fun getLibraChainFiatBalance(address: String) = checkResponse {
        mViolasApi.getLibraChainFiatBalance(address)
    }

    @Throws(RequestException::class)
    suspend fun getViolasChainFiatBalance(address: String) = checkResponse {
        mViolasApi.getViolasChainFiatBalance(address)
    }

    @Throws(RequestException::class)
    suspend fun getMarketSupportCurrencies() =
        checkResponse {
            mViolasApi.getMarketSupportCurrencies()
        }

    @Throws(RequestException::class)
    suspend fun exchangeSwapTrial(
        amount: Long,
        currencyIn: String,
        currencyOut: String
    ) = checkResponse {
        mViolasApi.exchangeSwapTrial(amount, currencyIn, currencyOut)
    }

    @Throws(RequestException::class)
    suspend fun getMarketSwapRecords(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        checkResponse {
            mViolasApi.getMarketSwapRecords(address, pageSize, offset)
        }

    @Throws(RequestException::class)
    suspend fun getMarketPoolRecords(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        checkResponse {
            mViolasApi.getMarketPoolRecords(address, pageSize, offset)
        }

    @Throws(RequestException::class)
    suspend fun getUserPoolInfo(
        address: String
    ) =
        checkResponse {
            mViolasApi.getUserPoolInfo(address)
        }

    @Throws(RequestException::class)
    suspend fun removePoolLiquidityEstimate(
        address: String,
        tokenAName: String,
        tokenBName: String,
        liquidityAmount: String
    ) =
        checkResponse(dataNullableOnSuccess = false) {
            mViolasApi.removePoolLiquidityEstimate(
                address,
                tokenAName,
                tokenBName,
                liquidityAmount
            )
        }

    @Throws(RequestException::class)
    suspend fun addPoolLiquidityEstimate(
        tokenAName: String,
        tokenBName: String,
        tokenAAmount: String
    ) =
        checkResponse(dataNullableOnSuccess = false) {
            mViolasApi.addPoolLiquidityEstimate(
                tokenAName,
                tokenBName,
                tokenAAmount
            )
        }
}
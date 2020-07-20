package org.palliums.violascore.http

import androidx.annotation.StringDef
import com.palliums.net.checkResponse

/**
 * Created by elephant on 2020/3/30 22:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasRpcRepository(private val mViolasRpcApi: ViolasRpcApi) {

    @StringDef(
        Method.SUBMIT, Method.GET_ACCOUNT_STATE
    )
    annotation class Method {
        companion object {
            const val SUBMIT = "submit"
            const val GET_ACCOUNT_TRANSACTION = "get_account_transaction"
            const val GET_ACCOUNT_STATE = "get_account_state"
            const val GET_CURRENCIES = "get_currencies"
        }
    }

    suspend fun getCurrencies() = checkResponse {
        mViolasRpcApi.getCurrencies(
            RequestDTO(
                method = Method.GET_CURRENCIES,
                params = listOf()
            )
        )
    }

    suspend fun getAccountState(
        address: String
    ) = checkResponse {
        mViolasRpcApi.getAccountState(
            RequestDTO(
                method = Method.GET_ACCOUNT_STATE,
                params = listOf(address)
            )
        )
    }

    suspend fun getTransaction(
        address: String,
        sequenceNumber: Long,
        bool: Boolean = true
    ) =
        checkResponse {
            mViolasRpcApi.getTransaction(
                RequestDTO(
                    method = Method.GET_ACCOUNT_TRANSACTION,
                    params = listOf(address, sequenceNumber, bool)
                )
            )
        }

    suspend fun submitTransaction(
        hexSignedTransaction: String
    ) =
        checkResponse {
            mViolasRpcApi.submitTransaction(
                RequestDTO(
                    method = Method.SUBMIT,
                    params = listOf(hexSignedTransaction)
                )
            )
        }

}
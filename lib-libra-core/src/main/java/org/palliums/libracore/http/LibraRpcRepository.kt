package org.palliums.libracore.http

import androidx.annotation.StringDef
import com.palliums.net.await

/**
 * Created by elephant on 2020/3/30 22:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraRpcRepository(private val mLibraRpcApi: LibraRpcApi) {

    @StringDef(
        Method.SUBMIT, Method.GET_ACCOUNT_STATE
    )
    annotation class Method {
        companion object {
            const val SUBMIT = "submit"
            const val GET_ACCOUNT_TRANSACTION = "get_account_transaction"
            const val GET_ACCOUNT_STATE = "get_account"
            const val GET_CURRENCIES = "get_currencies"
        }
    }

    suspend fun getAccountState(
        address: String
    ) =
        mLibraRpcApi.getAccountState(
            RequestDTO(
                method = Method.GET_ACCOUNT_STATE,
                params = listOf(address)
            )
        ).await()

    suspend fun getCurrencies() =
        mLibraRpcApi.getCurrencies(
            RequestDTO(
                method = Method.GET_CURRENCIES,
                params = listOf()
            )
        ).await()

    suspend fun getTransaction(
        address: String,
        sequenceNumber: Long,
        bool: Boolean = true
    ) =
        mLibraRpcApi.getTransaction(
            RequestDTO(
                method = Method.GET_ACCOUNT_TRANSACTION,
                params = listOf(address, sequenceNumber, bool)
            )
        ).await()

    suspend fun submitTransaction(
        hexSignedTransaction: String
    ) =
        mLibraRpcApi.submitTransaction(
            RequestDTO(
                method = Method.SUBMIT,
                params = listOf(hexSignedTransaction)
            )
        ).await()

}
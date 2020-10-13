package org.palliums.violascore.http

import okhttp3.OkHttpClient
import org.palliums.lib.jsonrpc.JsonRPCRequestDTO
import org.palliums.lib.jsonrpc.RPCService

/**
 * Created by elephant on 2020/3/30 22:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasRpcRepository(okHttpClient: OkHttpClient, libraChainNode: String) {

    private val mService by lazy {
        RPCService(libraChainNode, okHttpClient)
    }

    suspend fun getAccountState(
        address: String
    ) = mService.call<AccountStateDTO>(
        JsonRPCRequestDTO(
            "get_account",
            listOf(address)
        )
    )

    suspend fun getCurrencies() = mService.call<CurrenciesDTO>(
        JsonRPCRequestDTO(
            "get_currencies",
            listOf()
        )
    )

    suspend fun getTransaction(
        address: String,
        sequenceNumber: Long,
        bool: Boolean = true
    ) = mService.call<GetTransactionDTO>(
        JsonRPCRequestDTO(
            "get_account_transaction",
            listOf(address, sequenceNumber, bool)
        )
    )

    suspend fun submitTransaction(
        hexSignedTransaction: String
    ) = mService.call<Any>(
        JsonRPCRequestDTO(
            "submit",
            listOf(hexSignedTransaction)
        )
    )

}
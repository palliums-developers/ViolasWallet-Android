package org.palliums.libracore.http

import androidx.annotation.StringDef
import com.google.gson.Gson
import com.palliums.net.checkResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2020/3/30 22:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraRepository(private val mLibraApi: LibraApi) {

    @StringDef(
        Method.SUBMIT, Method.GET_ACCOUNT_STATE
    )
    annotation class Method {
        companion object {
            const val SUBMIT = "submit"
            const val GET_ACCOUNT_STATE = "get_account_state"
        }
    }

    suspend fun getAccountState(
        addresses: List<String>
    ) =
        checkResponse {
            val requestDTO = RequestDTO(
                method = Method.GET_ACCOUNT_STATE,
                params = addresses
            )

            val requestBody = Gson().toJson(requestDTO)
                .toRequestBody("application/json".toMediaTypeOrNull())

            mLibraApi.getAccountState(requestBody)
        }

    suspend fun submitTransaction(
        signedTransactions: List<String>
    ) =
        checkResponse {
            val requestDTO = RequestDTO(
                method = Method.SUBMIT,
                params = signedTransactions
            )

            val requestBody = Gson().toJson(requestDTO)
                .toRequestBody("application/json".toMediaTypeOrNull())

            mLibraApi.submitTransaction(requestBody)
        }
}
package org.palliums.libracore.http

import androidx.annotation.StringDef
import com.palliums.net.checkResponse

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
        address: String
    ) =
        checkResponse {
            mLibraApi.getAccountState(
                RequestDTO(
                    method = Method.GET_ACCOUNT_STATE,
                    params = listOf(address)
                )
            )
        }

    suspend fun submitTransaction(
        hexSignedTransaction: String
    ) =
        checkResponse {
            mLibraApi.submitTransaction(
                RequestDTO(
                    method = Method.SUBMIT,
                    params = listOf(hexSignedTransaction)
                )
            )
        }
}
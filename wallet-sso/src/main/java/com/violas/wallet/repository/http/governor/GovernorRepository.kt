package com.violas.wallet.repository.http.governor

import com.palliums.net.checkResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2020/2/26 20:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class GovernorRepository(private val api: GovernorApi) {

    suspend fun signUpGovernor(
        walletAddress: String, name: String, txid: String
    ) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "name":"$name",
    "txid":"$txid"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.signUpGovernor(requestBody)
        }

    suspend fun getGovernorInfo(walletAddress: String) =
        checkResponse(dataNullableOnSuccess = false) {
            api.getGovernorInfo(walletAddress)
        }

    suspend fun getVStakeAddress() =
        checkResponse(dataNullableOnSuccess = false) {
            api.getVStakeAddress()
        }

    suspend fun getSSOApplications(
        walletAddress: String, pageSize: Int, offset: Int
    ) =
        checkResponse {
            api.getSSOApplications(walletAddress, pageSize, offset)
        }

    suspend fun approvalSSOApplication(
        pass: Boolean, newTokenAddress: String, ssoWalletAddress: String
    ) =
        checkResponse {
            val requestBody = """{
    "approval_status":${if (pass) 1 else 2},
    "module_address":"${if (pass) newTokenAddress else ""}",
    "wallet_address":"$ssoWalletAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.approvalSSOApplication(requestBody)
        }

    suspend fun changeSSOApplicationToMinted(ssoWalletAddress: String) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$ssoWalletAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.changeSSOApplicationToMinted(requestBody)
        }
}

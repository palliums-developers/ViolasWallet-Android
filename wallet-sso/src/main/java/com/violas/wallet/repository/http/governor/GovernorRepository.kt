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

    /**
     * 注册州长
     */
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

    /**
     * 获取州长信息
     */
    suspend fun getGovernorInfo(walletAddress: String) =
        checkResponse(dataNullableOnSuccess = false) {
            api.getGovernorInfo(walletAddress)
        }

    /**
     * 更新子账户个数
     */
    suspend fun updateSubAccountCount(walletAddress: String, subAccountCount: Int) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "subaccount_count":$subAccountCount
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.updateGovernorInfo(requestBody)
        }

    /**
     * 更新州长名称
     */
    suspend fun updateGovernorName(walletAddress: String, name: String) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "name":"$name"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.updateGovernorInfo(requestBody)
        }

    /**
     * 获取vstake地址
     */
    suspend fun getVStakeAddress() =
        checkResponse(dataNullableOnSuccess = false) {
            api.getVStakeAddress()
        }

    /**
     * 获取SSO发币申请
     */
    suspend fun getSSOApplications(
        walletAddress: String, pageSize: Int, offset: Int
    ) =
        checkResponse {
            api.getSSOApplications(walletAddress, pageSize, offset)
        }

    /**
     * 审批SSO发币申请
     */
    suspend fun approvalSSOApplication(
        pass: Boolean, newTokenAddress: String, ssoWalletAddress: String, walletLayersNumber: Int
    ) =
        checkResponse {
            val requestBody = """{
    "approval_status":${if (pass) 1 else 2},
    "module_address":"${if (pass) newTokenAddress else ""}",
    "wallet_address":"$ssoWalletAddress",
    "subaccount_count":$walletLayersNumber
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.approvalSSOApplication(requestBody)
        }

    /**
     * 改变SSO发币申请状态为已铸币
     */
    suspend fun changeSSOApplicationToMinted(ssoWalletAddress: String) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$ssoWalletAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.changeSSOApplicationToMinted(requestBody)
        }
}

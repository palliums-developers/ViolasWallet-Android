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
        walletAddress: String,
        publicKey: String,
        name: String,
        txid: String,
        toxid: String = ""
    ) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "public_key":"$publicKey",
    "name":"$name",
    "txid":"$txid",
    "toxid":"$toxid"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.signUpGovernor(requestBody)
        }

    /**
     * 获取州长信息
     */
    suspend fun getGovernorInfo(
        walletAddress: String
    ) =
        checkResponse(2011) {
            api.getGovernorInfo(walletAddress)
        }

    /**
     * 更新州长名称
     */
    suspend fun updateGovernorName(
        walletAddress: String,
        name: String
    ) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "name":"$name"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.updateGovernorInfo(requestBody)
        }

    /**
     * 更新州长申请状态为published
     */
    suspend fun updateGovernorApplicationToPublished(
        walletAddress: String
    ) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$walletAddress",
    "is_handle":3
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.updateGovernorApplicationToPublished(requestBody)
        }

    /**
     * 获取SSO申请消息
     */
    suspend fun getSSOApplicationMsgs(
        walletAddress: String, pageSize: Int, offset: Int
    ) =
        checkResponse {
            api.getSSOApplicationMsgs(walletAddress, pageSize, offset)
        }

    /**
     * 获取SSO申请详情
     */
    suspend fun getSSOApplicationDetails(
        ssoApplicationId: String
    ) =
        checkResponse {
            api.getSSOApplicationDetails(ssoApplicationId)
        }

    /**
     * 获取审核SSO申请不通过原因列表
     */
    suspend fun getUnapproveReasons() =
        checkResponse(dataNullableOnSuccess = false) {
            api.getUnapproveReasons()
        }.data!!

    /**
     * 审核不通过SSO申请
     */
    suspend fun unapproveSSOApplication(
        ssoApplicationId: String,
        ssoWalletAddress: String,
        reasonType: Int,
        reasonRemark: String = ""
    ) =
        checkResponse {
            val requestBody = """{
    "id":"$ssoApplicationId",
    "wallet_address":"$ssoWalletAddress",
    "approval_status":2,
    "reason_type":$reasonType,
    "reason_remark":$reasonRemark
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.approvalSSOApplication(requestBody)
        }

    /**
     * 申请铸币权
     */
    suspend fun applyForMintPower(
        governorWalletAddress: String,
        ssoApplicationId: String,
        ssoWalletAddress: String
    ) =
        checkResponse {
            val requestBody = """{
    "wallet_address":"$governorWalletAddress",
    "id":"$ssoApplicationId",
    "sso_wallet_address":"$ssoWalletAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.applyForMintPower(requestBody)
        }

    /**
     * 审核通过SSO申请
     */
    suspend fun approveSSOApplication(
        ssoApplicationId: String,
        ssoWalletAddress: String
    ) =
        checkResponse {
            val requestBody = """{
    "id":"$ssoApplicationId",
    "wallet_address":"$ssoWalletAddress",
    "approval_status":1
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.approvalSSOApplication(requestBody)
        }

    /**
     * 改变SSO申请状态为已铸币
     */
    suspend fun changeSSOApplicationToMinted(
        ssoApplicationId: String,
        ssoWalletAddress: String
    ) =
        checkResponse {
            val requestBody = """{
    "id":"$ssoApplicationId",
    "wallet_address":"$ssoWalletAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.changeSSOApplicationToMinted(requestBody)
        }

    /**
     * 登录桌面端钱包
     */
    suspend fun loginDesktop(
        walletAddress: String,
        type: Int,
        signedSessionId: String
    ) =
        checkResponse {
            val requestBody = """{
    "address":"$walletAddress",
    "type":$type,
    "session_id":"$signedSessionId"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.loginDesktop(requestBody)
        }
}

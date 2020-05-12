package com.violas.wallet.repository.http.governor

import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.ui.selectCountryArea.getCountryArea
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
        walletAddress: String,
        ssoApplicationId: String
    ): SSOApplicationDetailsDTO? {
        val details =
            checkResponse {
                api.getSSOApplicationDetails(walletAddress, ssoApplicationId)
            }.data

        details?.let {
            if (it.applicationStatus >= SSOApplicationState.CHAIRMAN_APPROVED
                && it.tokenIdx == null
            ) {
                throw RequestException.responseDataException("Token id cannot be null")
            } else if ((it.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED
                        || it.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED)
                && it.unapprovedReason.isNullOrEmpty()
                && it.unapprovedRemarks.isNullOrEmpty()
            ) {
                throw RequestException.responseDataException(
                    "Unapproved reasons and unapproved remarks cannot all be empty"
                )
            }

            val countryArea = getCountryArea(it.countryCode)
            it.countryName = countryArea.countryName
        }

        return details
    }

    /**
     * 获取审核SSO申请不通过原因列表
     */
    suspend fun getUnapproveReasons(): List<UnapproveReasonDTO> {
        val remoteReasons =
            checkResponse(dataNullableOnSuccess = false) {
                api.getUnapproveReasons()
            }.data!!

        return remoteReasons.map {
            UnapproveReasonDTO(it.key, it.value)
        }
    }

    /**
     * 审核不通过SSO申请
     */
    suspend fun unapproveSSOApplication(
        ssoApplicationId: String,
        issuerWalletAddress: String,
        reasonType: Int,
        reasonRemarks: String = ""
    ) =
        checkResponse {
            val requestBody = """{
    "id":"$ssoApplicationId",
    "address":"$issuerWalletAddress",
    "status":${SSOApplicationState.GOVERNOR_UNAPPROVED},
    "reason":$reasonType,
    "remarks":"$reasonRemarks"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.submitSSOApplicationApprovalResults(requestBody)
        }

    /**
     * 提交SSO申请审批结果
     */
    suspend fun submitSSOApplicationApprovalResults(
        ssoApplicationId: String,
        issuerWalletAddress: String,
        @SSOApplicationState
        approvalResults: Int
    ) =
        checkResponse {
            val requestBody = """{
    "id":"$ssoApplicationId",
    "address":"$issuerWalletAddress",
    "status":$approvalResults
}""".toRequestBody("application/json".toMediaTypeOrNull())
            api.submitSSOApplicationApprovalResults(requestBody)
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

package com.violas.wallet.repository.http.sso

import com.palliums.violas.http.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SSORepository(private val ssoApi: SSOApi) {
    private fun <T> checkResponse(response: Response<T>): Response<T>? {
        if (response.errorCode == 2000) {
            return response
        }
        return null
    }

    suspend fun applyForIssuing(
        walletAddress: String,
        tokenType: String,
        amount: Long,
        tokenValue: String,
        tokenName: String,
        reserivePhotoUrl: String,
        accountInfoPhotoPositiveUrl: String,
        accountInfoPhotoBackUrl: String
    ): Response<Any>? {
        val toRequestBody = """{
    "wallet_address":"$walletAddress",
    "token_type":"$tokenType",
    "amount":"$amount",
    "token_value":"$tokenValue",
    "token_name":"$tokenName",
    "reserive_photo_url":"$reserivePhotoUrl",
    "account_info_photo_positive_url":"$accountInfoPhotoPositiveUrl",
    "account_info_photo_back_url":"$accountInfoPhotoBackUrl"
}""".toRequestBody("application/json".toMediaTypeOrNull())
        return try {
            checkResponse(ssoApi.applyForIssuing(toRequestBody))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun selectApplyForStatus(address: String): Response<ApplyForStatusDTO>? {
        return try {
            checkResponse(ssoApi.selectApplyForStatus(address))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun selectPublishStatus(address: String): Response<Any>? {
        val toRequestBody = """{
    "address":"$address"
}""".toRequestBody("application/json".toMediaTypeOrNull())
        return try {
            checkResponse(ssoApi.selectPublishStatus(toRequestBody))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadImage(file: File): Response<String>? {
        val asRequestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val createFormData = MultipartBody.Part.createFormData("photo", file.name, asRequestBody)
        return try {
            checkResponse(ssoApi.uploadImage(createFormData))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendVerifyCode(receiver: String): Response<Any>? {
        return try {
            checkResponse(ssoApi.sendVerifyCode(receiver))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun verifyCode(receiver: String, code: String): Response<Any>? {
        val toRequestBody = """{
    "receiver":"$receiver",
    "code":$code
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return try {
            checkResponse(ssoApi.verifyCode(toRequestBody))
        } catch (e: Exception) {
            null
        }
    }
}
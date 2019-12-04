package com.violas.wallet.repository.http.sso

import com.palliums.net.RequestException
import com.palliums.net.checkResponse
import com.palliums.violas.http.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SSORepository(private val ssoApi: SSOApi) {

    /**
     * 绑定身份证信息
     */
    suspend fun bindIdNumber(
        walletAddress: String,
        name: String,
        countryCode: String,
        idNumber: String,
        idPhotoPositiveUrl: String,
        idPhotoBackUrl: String
    ): Response<Any> {
        val toRequestBody = """{
    "wallet_address":"$walletAddress",
    "name":"$name",
    "country":"$countryCode",
    "id_number":"$idNumber",
    "id_photo_positive_url":"$idPhotoPositiveUrl",
    "id_photo_back_url":"$idPhotoBackUrl"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            ssoApi.bindIdNumber(toRequestBody)
        }
    }

    /**
     * 获取用户基本信息，包括绑定状态
     */
    suspend fun loadUserInfo(address: String): Response<UserInfoDTO> {
        return checkResponse(2005) {
            ssoApi.loadUserInfo(address)
        }
    }

    /**
     * 申请发行稳定币
     */
    suspend fun applyForIssuing(
        walletAddress: String,
        tokenType: String,
        amount: Long,
        tokenValue: Float,
        tokenName: String,
        reservePhotoUrl: String,
        accountInfoPhotoPositiveUrl: String,
        accountInfoPhotoBackUrl: String,
        phoneVerifyCode: String,
        emailVerifyCode: String
    ): Response<Any>? {
        val toRequestBody = """{
    "wallet_address":"$walletAddress",
    "token_type":"$tokenType",
    "amount":$amount,
    "token_value":$tokenValue,
    "token_name":"$tokenName",
    "reserve_photo_url":"$reservePhotoUrl",
    "account_info_photo_positive_url":"$accountInfoPhotoPositiveUrl",
    "account_info_photo_back_url":"$accountInfoPhotoBackUrl",
    "phone_verify_code":$phoneVerifyCode,
    "email_verify_code":$emailVerifyCode
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return try {
            checkResponse(2003) {
                ssoApi.applyForIssuing(toRequestBody)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查询审批状态
     */
    suspend fun selectApplyForStatus(address: String): Response<ApplyForStatusDTO>? {
        return try {
            checkResponse(2005) {
                ssoApi.selectApplyForStatus(address)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 更改Publish状态
     */
    suspend fun changePublishStatus(address: String): Response<Any>? {
        val toRequestBody = """{
    "address":"$address"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return try {
            checkResponse {
                ssoApi.changePublishStatus(toRequestBody)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 上传图片
     */
    suspend fun uploadImage(file: File): Response<String>? {
        val asRequestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val createFormData =
            MultipartBody.Part.createFormData("photo", file.name, asRequestBody)

        return try {
            checkResponse {
                ssoApi.uploadImage(createFormData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 上传图片
     */
    suspend fun uploadImage2(file: File): Response<String> {
        val asRequestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val createFormData =
            MultipartBody.Part.createFormData("photo", file.name, asRequestBody)

        return checkResponse {
            ssoApi.uploadImage(createFormData).also {
                if (it.data.isNullOrEmpty()) {
                    throw RequestException.responseDataException()
                }
            }
        }
    }

    /**
     * 获取手机验证码
     */
    suspend fun sendPhoneVerifyCode(
        address: String,
        phoneNumber: String,
        areaCode: String
    ): Response<Any> {
        val toRequestBody = """{
    "address":"$address",
    "receiver":"$phoneNumber",
    "phone_local_number":"+$areaCode"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            ssoApi.sendVerifyCode(toRequestBody)
        }
    }

    /**
     * 获取邮箱验证码
     */
    suspend fun sendEmailVerifyCode(address: String, emailAddress: String): Response<Any> {
        val toRequestBody = """{
    "address":"$address",
    "receiver":"$emailAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            ssoApi.sendVerifyCode(toRequestBody)
        }
    }

    /**
     * 绑定手机
     */
    suspend fun bindPhone(
        address: String,
        phoneNumber: String,
        areaCode: String,
        verificationCode: String
    ): Response<Any> {
        val toRequestBody = """{
    "address":"$address",
    "receiver":"$phoneNumber",
    "phone_local_number":"+$areaCode",
    "code":"$verificationCode"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            ssoApi.bind(toRequestBody)
        }
    }

    /**
     * 绑定邮箱
     */
    suspend fun bindEmail(
        address: String,
        emailAddress: String,
        verificationCode: String
    ): Response<Any> {
        val toRequestBody = """{
    "address":"$address",
    "receiver":"$emailAddress",
    "code":"$verificationCode"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return checkResponse {
            ssoApi.bind(toRequestBody)
        }
    }
}
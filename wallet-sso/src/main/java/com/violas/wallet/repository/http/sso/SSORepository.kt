package com.violas.wallet.repository.http.sso

import com.palliums.content.ContextProvider
import com.palliums.net.checkResponse
import com.palliums.utils.getImageName
import com.palliums.violas.http.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import top.zibin.luban.Luban
import java.io.File

class ImageCompressionFailedException : RuntimeException()

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
        governorAddress: String,
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
    "email_verify_code":$emailVerifyCode,
    "governor_address":"$governorAddress"
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
    suspend fun selectApplyForStatus(
        address: String,
        handleException: Boolean = true
    ): Response<ApplyForStatusDTO>? {
        return try {
            checkResponse(2006) {
                ssoApi.selectApplyForStatus(address)
            }
        } catch (e: Exception) {
            e.printStackTrace()

            if (handleException) {
                null
            } else {
                throw e
            }
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
    suspend fun uploadImage(nativeFile: File): Response<String> = withContext(Dispatchers.IO) {
        val fileList =
            Luban.with(ContextProvider.getContext()).load(nativeFile).ignoreBy(4 * 1024).get()
        val file = if (fileList.size >= 1) {
            fileList[0]
        } else {
            throw ImageCompressionFailedException()
        }

        val imageName = file.getImageName() ?: "${file.name}.jpg"

        val asRequestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val createFormData =
            MultipartBody.Part.createFormData("photo", imageName, asRequestBody)

        checkResponse(dataNullableOnSuccess = false) {
            ssoApi.uploadImage(createFormData)
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
    "phone_local_number":"${if (areaCode.startsWith("+")) areaCode else "+$areaCode"}"
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
    "phone_local_number":"${if (areaCode.startsWith("+")) areaCode else "+$areaCode"}",
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

    /**
     * 获取州长列表
     */
    suspend fun getGovernorList(offset: Int = 0, limit: Int = 100) = withContext(Dispatchers.IO) {
        checkResponse {
            ssoApi.getGovernorList(offset, limit)
        }
    }
}
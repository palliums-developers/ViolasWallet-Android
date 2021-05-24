package com.violas.wallet.repository.http.issuer

import com.palliums.content.ContextProvider
import com.palliums.exceptions.RequestException
import com.palliums.net.await
import com.palliums.utils.getImageName
import com.palliums.violas.http.Response
import com.violas.wallet.biz.SSOApplicationState
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import top.zibin.luban.Luban
import java.io.File

class ImageCompressionFailedException : RuntimeException()

class SSORepository(private val ssoApi: IssuerApi) {

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

        return ssoApi.bindIdNumber(toRequestBody).await()
    }

    /**
     * 获取用户基本信息，包括绑定状态
     */
    suspend fun loadUserInfo(walletAddress: String): Response<UserInfoDTO> {
        // {"code":2004,"message":"SSO info does not exist."}
        return ssoApi.loadUserInfo(walletAddress).await(2004)
    }

    /**
     * 查询发行商申请发行SSO的摘要信息
     */
    suspend fun queryApplyForSSOSummary(
        walletAddress: String
    ): ApplyForSSOSummaryDTO? {
        // {"code":2005,"message":"Token info does not exist."}
        val summary =
            ssoApi.queryApplyForSSOSummary(walletAddress)
                .await(2005).data

        summary?.let {
            if (it.applicationStatus < SSOApplicationState.CHAIRMAN_UNAPPROVED
                || it.applicationStatus > SSOApplicationState.GOVERNOR_MINTED
            ) {
                throw RequestException.responseDataError(
                    "Unknown approval status ${it.applicationStatus}"
                )
            } else if (it.applicationStatus >= SSOApplicationState.CHAIRMAN_APPROVED
                && it.tokenIdx == null
            ) {
                throw RequestException.responseDataError("Token id cannot be null")
            }
        }

        return summary
    }

    /**
     * 获取发行商申请发行SSO的详细信息
     */
    suspend fun getApplyForSSODetails(
        walletAddress: String
    ): ApplyForSSODetailsDTO? {
        val details =
            ssoApi.getApplyForSSODetails(walletAddress)
                .await(2005).data

        details?.let {
            if (it.applicationStatus < SSOApplicationState.CHAIRMAN_UNAPPROVED
                || it.applicationStatus > SSOApplicationState.GOVERNOR_MINTED
            ) {
                throw RequestException.responseDataError(
                    "Unknown approval status ${it.applicationStatus}"
                )
            } else if (it.applicationStatus >= SSOApplicationState.CHAIRMAN_APPROVED
                && it.tokenIdx == null
            ) {
                throw RequestException.responseDataError("Token id cannot be null")
            } else if ((it.applicationStatus == SSOApplicationState.GOVERNOR_UNAPPROVED
                        || it.applicationStatus == SSOApplicationState.CHAIRMAN_UNAPPROVED)
                && it.unapprovedReason.isNullOrEmpty()
                && it.unapprovedRemarks.isNullOrEmpty()
            ) {
                throw RequestException.responseDataError(
                    "Unapproved reasons and unapproved remarks cannot all be empty"
                )
            }

            val countryArea = getCountryArea(it.countryCode)
            it.countryName = countryArea.countryName
        }

        return details
    }

    /**
     * 申请发行稳定币
     */
    suspend fun applyForIssueToken(
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

        return ssoApi.applyForIssueToken(toRequestBody).await(2003)
    }

    /**
     * 更改申请发行SSO的状态为 published
     */
    suspend fun changeApplyForSSOToPublished(
        walletAddress: String,
        ssoApplicationId: String
    ): Response<Any>? {
        val toRequestBody = """{
    "address":"$walletAddress",
    "id":"$ssoApplicationId"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return ssoApi.changeApplyForSSOToPublished(toRequestBody).await()
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

        ssoApi.uploadImage(createFormData).await(dataNullableOnSuccess = false)
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

        return ssoApi.sendVerifyCode(toRequestBody).await()
    }

    /**
     * 获取邮箱验证码
     */
    suspend fun sendEmailVerifyCode(address: String, emailAddress: String): Response<Any> {
        val toRequestBody = """{
    "address":"$address",
    "receiver":"$emailAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())

        return ssoApi.sendVerifyCode(toRequestBody).await()
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

        return ssoApi.bind(toRequestBody).await()
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

        return ssoApi.bind(toRequestBody).await()
    }

    /**
     * 获取州长列表
     */
    suspend fun getGovernorList(
        offset: Int = 0, limit: Int = 100
    ) = withContext(Dispatchers.IO) {
        ssoApi.getGovernorList(offset, limit).await()
    }
}
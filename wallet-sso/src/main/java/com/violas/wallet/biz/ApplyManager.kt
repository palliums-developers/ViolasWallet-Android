package com.violas.wallet.biz

import com.palliums.violas.http.Response
import com.violas.wallet.repository.DataRepository.getSSOService
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import java.io.File

class ApplyManager {
    private val mSSOService by lazy {
        getSSOService()
    }

    suspend fun getApplyStatus(address: String): Response<ApplyForStatusDTO>? {
        return mSSOService.selectApplyForStatus(address)
    }

    suspend fun uploadImage(file: File) =
        mSSOService.uploadImage(file)

    suspend fun changePublishStatus(address: String) = mSSOService.changePublishStatus(address)

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
    ) = mSSOService.applyForIssuing(
        walletAddress,
        tokenType,
        amount,
        tokenValue,
        tokenName,
        reservePhotoUrl,
        accountInfoPhotoPositiveUrl,
        accountInfoPhotoBackUrl,
        phoneVerifyCode,
        emailVerifyCode
    )

}

package com.violas.wallet.biz

import com.palliums.violas.http.Response
import com.violas.wallet.repository.DataRepository.getSSOService
import com.violas.wallet.repository.http.sso.ApplyForStatusDTO
import java.io.File
import java.math.BigDecimal

class SSOManager {

    private val mSSOService by lazy {
        getSSOService()
    }

    suspend fun getSSOApplicationDetails(
        address: String
    ) =
        mSSOService.getSSOApplicationDetails(address)

    suspend fun getApplyStatus(
        address: String
    ): Response<ApplyForStatusDTO>? {
        return mSSOService.selectApplyForStatus(address)
    }

    suspend fun uploadImage(file: File) =
        mSSOService.uploadImage(file)

    suspend fun changePublishStatus(address: String) = mSSOService.changePublishStatus(address)

    suspend fun applyForIssuing(
        walletAddress: String,
        tokenType: String,
        amount: BigDecimal,
        tokenValue: Float,
        tokenName: String,
        reservePhotoUrl: String,
        accountInfoPhotoPositiveUrl: String,
        accountInfoPhotoBackUrl: String,
        governorAddress: String,
        phoneVerifyCode: String,
        emailVerifyCode: String
    ) = mSSOService.applyForIssuing(
        walletAddress,
        tokenType,
        amount.multiply(BigDecimal("1000000")).toLong(),
        tokenValue,
        tokenName,
        reservePhotoUrl,
        accountInfoPhotoPositiveUrl,
        accountInfoPhotoBackUrl,
        governorAddress,
        phoneVerifyCode,
        emailVerifyCode
    )

}

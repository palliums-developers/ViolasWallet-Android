package com.violas.wallet.biz

import com.violas.wallet.repository.DataRepository
import java.io.File
import java.math.BigDecimal

class IssuerManager {

    private val mIssuerService by lazy { DataRepository.getIssuerService() }

    suspend fun uploadImage(file: File) =
        mIssuerService.uploadImage(file)

    suspend fun changeApplyForSSOToPublished(
        walletAddress: String,
        ssoApplicationId: String
    ) =
        mIssuerService.changeApplyForSSOToPublished(walletAddress, ssoApplicationId)

    suspend fun applyForIssueToken(
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
    ) =
        mIssuerService.applyForIssueToken(
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

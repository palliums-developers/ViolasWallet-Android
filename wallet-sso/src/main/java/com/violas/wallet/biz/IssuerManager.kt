package com.violas.wallet.biz

import com.violas.wallet.repository.DataRepository
import java.io.File
import java.math.BigDecimal

class IssuerManager {

    private val mIssuerService by lazy { DataRepository.getIssuerService() }

    suspend fun uploadImage(file: File) =
        mIssuerService.uploadImage(file)

    suspend fun changePublishStatus(address: String) =
        mIssuerService.changePublishStatus(address)

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
    ) =
        mIssuerService.applyForIssueSSO(
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

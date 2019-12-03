package com.violas.wallet.repository.http.sso

import com.google.gson.annotations.SerializedName

data class ApplyForStatusDTO(
    val amount: Int,
    val approval_status: Int,
    val token_name: String
)

data class UserInfoDTO(
    @SerializedName("country")
    val countryCode: String?,
    @SerializedName("name")
    val idName: String?,
    @SerializedName("id_number")
    val idNumber: String?,
    @SerializedName("id_photo_positive_url")
    val idPhotoFrontUrl: String?,
    @SerializedName("id_photo_back_url")
    val idPhotoBackUrl: String?,
    @SerializedName("email_address")
    val emailAddress: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("phone_local_number")
    val phoneAreaCode: String?
)
package com.violas.wallet.repository.http.sso

data class ApplyForStatusDTO(
    val amount: Int,
    val approval_status: Int,
    val token_name: String
)

data class UserInfoDTO(
    val country: String,
    val email_address: String,
    val id_number: String,
    val id_photo_back_url: String,
    val id_photo_positive_url: String,
    val name: String,
    val phone_number: String,
    val wallet_address: String,
    val phone_local_number: String
)
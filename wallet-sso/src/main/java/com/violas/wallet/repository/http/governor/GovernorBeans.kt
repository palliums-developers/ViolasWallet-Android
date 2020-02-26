package com.violas.wallet.repository.http.governor

import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 2020/2/26 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class GovernorInfoDTO(
    @SerializedName("wallet_address")
    val walletAddress: String,
    val name: String,
    @SerializedName("subaccount_count")
    val subAccountCount: Int
)

data class SSOApplicationDTO(
    @SerializedName("wallet_address")
    val walletAddress: String,                      // 申请者的钱包地址
    @SerializedName("name")
    val idName: String,                             // 申请者的身份姓名
    @SerializedName("id_number")
    val idNumber: String,                           // 申请者的证件号码
    @SerializedName("id_photo_positive_url")
    val idPhotoPositiveUrl: String,                 // 申请者的证件正面照片url
    @SerializedName("id_photo_back_url")
    val idPhotoBackUrl: String,                     // 申请者的证件背面照片url
    @SerializedName("name")
    val countryCode: String,                        // 申请者的国家码（需要本地换算成国家名称）
    @SerializedName("email_address")
    val emailAddress: String,                       // 申请者的邮箱地址
    @SerializedName("phone_number")
    val phoneNumber: String,                        // 申请者的手机号码
    @SerializedName("phone_local_number")
    val phoneAreaCode: String,                      // 申请者的手机区号

    @SerializedName("token_type")
    val tokenType: String,                          // 法币种类
    @SerializedName("amount")
    val tokenAmount: String,                        // 法币数量
    @SerializedName("token_name")
    val tokenName: String,                          // 稳定币名称
    @SerializedName("token_value")
    val tokenValue: Int,                            // 稳定币价值
    @SerializedName("reserve_photo_url")
    val reservePhotoUrl: String,                    // 储备金照片url
    @SerializedName("account_info_photo_positive_url")
    val bankChequePhotoPositiveUrl: String,         // 银行支付支票正面照片url
    @SerializedName("account_info_photo_back_url")
    val bankChequePhotoBackUrl: String,             // 银行支付支票背面照片url
    @SerializedName("application_date")
    val applicationDate: Long,                      // 申请日期
    @SerializedName("expiration_date")
    val expirationDate: Long,                       // 申请失效日期
    @SerializedName("validity_period")
    val validityPeriod: Int,                        // 申请有效期（单位天数）
    @SerializedName("approval_status")
    val approvalStatus: Int                         // 审批状态 0: 未审批; 1: 审批通过; 2: 审批失败; 3: 已Publish; 4: 铸币成功
)
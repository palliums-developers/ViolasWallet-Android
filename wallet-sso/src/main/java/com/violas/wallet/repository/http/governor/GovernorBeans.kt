package com.violas.wallet.repository.http.governor

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 2020/2/26 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class GovernorInfoDTO(
    @SerializedName("wallet_address")
    val walletAddress: String = "",                 // 钱包地址
    @SerializedName("name")
    val name: String = "",                          // 州长名称
    @SerializedName("status")
    val applicationStatus: Int = -1,                // 申请州长状态 -1: no application; 0: not approved; 1: pass; 2: not pass; 3: published; 4: minted
    @SerializedName("subaccount_count")
    val subAccountCount: Long = 0                   // 子账户个数（用于派生铸币账户注册新的module）
)

data class SSOApplicationMsgDTO(
    @SerializedName("apply_id")
    val applicationId: String,                       // 申请ID
    @SerializedName("application_date")
    val applicationDate: Long,                      // 申请日期
    @SerializedName("approval_status")
    val applicationStatus: Int,                     // 申请发币状态 0: not approved; 1: pass; 2: not pass; 3: published; 4: minted
    @SerializedName("name")
    val applicantIdName: String                     // 申请人身份姓名
)

data class SSOApplicationDetailsDTO(
    @SerializedName("wallet_address")
    val ssoWalletAddress: String,                   // 申请者的钱包地址
    @SerializedName("name")
    val idName: String,                             // 申请者的身份姓名
    @SerializedName("id_number")
    val idNumber: String,                           // 申请者的证件号码
    @SerializedName("id_photo_positive_url")
    val idPhotoPositiveUrl: String,                 // 申请者的证件正面照片url
    @SerializedName("id_photo_back_url")
    val idPhotoBackUrl: String,                     // 申请者的证件背面照片url
    @SerializedName("country")
    val countryCode: String,                        // 申请者的国家码（需要本地换算成国家名称）
    @Expose(serialize = false, deserialize = false)
    var countryName: String = "",                   // 国家名称
    @SerializedName("email_address")
    val emailAddress: String,                       // 申请者的邮箱地址
    @SerializedName("phone_number")
    val phoneNumber: String,                        // 申请者的手机号码
    @SerializedName("phone_local_number")
    val phoneAreaCode: String,                      // 申请者的手机区号

    @SerializedName("token_type")
    val fiatCurrencyType: String,                   // 法币种类，即发币类型（如：USD、RMB）
    @SerializedName("amount")
    val tokenAmount: String,                        // 稳定币数量，即发行数量
    @SerializedName("token_name")
    val tokenName: String,                          // 稳定币名称
    @SerializedName("token_value")
    val tokenValue: Int,                            // 稳定币价值（单位发币种类）
    @SerializedName("module_address")
    val tokenAddress: String?,                      // 稳定币地址，即发行的module的address，在申请状态为pass之后才有
    @SerializedName("reserve_photo_url")
    val reservePhotoUrl: String,                    // 储备金照片url
    @SerializedName("account_info_photo_positive_url")
    val bankChequePhotoPositiveUrl: String,         // 银行支付支票正面照片url
    @SerializedName("account_info_photo_back_url")
    val bankChequePhotoBackUrl: String,             // 银行支付支票背面照片url
    @SerializedName("application_date")
    val applicationDate: Long,                      // 申请日期
    @SerializedName("validity_period")
    val applicationPeriod: Int,                     // 申请有效期（单位天数）
    @SerializedName("expiration_date")
    val expirationDate: Long,                       // 申请失效日期
    @SerializedName("approval_status")
    val applicationStatus: Int,                     // 申请发币状态 0: not approved; 1: pass; 2: not pass; 3: published; 4: minted
    @SerializedName("module_depth")
    val walletLayersNumber: Long = -1,              // 铸币账户的钱包层数，在申请状态为pass之后才有
    @SerializedName("apply_id")
    val applicationId: String                       // 申请ID
)
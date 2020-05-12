package com.violas.wallet.repository.http.issuer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.violas.wallet.biz.SSOApplicationState
import kotlinx.android.parcel.Parcelize

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

data class GovernorDTO(
    @SerializedName("name")
    val name: String,
    @SerializedName("wallet_address")
    val walletAddress: String
) : Parcelable {
    fun getNameFirst(): Char {
        return if (name.isNotEmpty())
            name.first()
        else
            '#'
    }

    constructor(source: Parcel) : this(
        source.readString() ?: "",
        source.readString() ?: ""
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(walletAddress)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GovernorDTO> = object : Parcelable.Creator<GovernorDTO> {
            override fun createFromParcel(source: Parcel): GovernorDTO = GovernorDTO(source)
            override fun newArray(size: Int): Array<GovernorDTO?> = arrayOfNulls(size)
        }
    }
}

/**
 * 发行商申请发行SSO的摘要信息
 */
@Parcelize
data class ApplyForSSOSummaryDTO(
    /**
     * -1: Audit timeout;
     * -2: Governor unapproved;
     * -3: Chairman unapproved;
     * 0: Issuer applying;
     * 1: Governor approved;
     * 2: Chairman approved;
     * 3: Governor transferred;
     * 4: Issuer published;
     * 5: Governor minted;
     */
    @SSOApplicationState
    @SerializedName("approval_status")
    val applicationStatus: Int,
    @SerializedName("id")
    val applicationId: String = "",
    @SerializedName("token_name")
    val tokenName: String = "",
    @SerializedName("token_id")
    val tokenIdx: Long? = null
) : Parcelable {

    companion object {

        fun newInstance(details: ApplyForSSODetailsDTO): ApplyForSSOSummaryDTO {
            return ApplyForSSOSummaryDTO(
                applicationStatus = details.applicationStatus,
                applicationId = details.applicationId,
                tokenName = details.tokenName,
                tokenIdx = details.tokenIdx
            )
        }
    }
}

/**
 * 发行商申请发行SSO的详细信息
 */
@Parcelize
data class ApplyForSSODetailsDTO(
    @SSOApplicationState
    @SerializedName("approval_status")
    var applicationStatus: Int,                     // 申请状态
    @SerializedName("id")
    val applicationId: String,                      // 申请ID

    @SerializedName("wallet_address")
    val issuerWalletAddress: String,                // 发行商的钱包地址
    @SerializedName("governor_address")
    val governorWalletAddress: String,              // 州长的钱包地址
    @SerializedName("governor_name")
    val governorName: String,                       // 州长的名称

    @SerializedName("name")
    val idName: String,                             // 申请者的身份姓名
    @SerializedName("id_number")
    val idNumber: String,                           // 申请者的证件号码
    @SerializedName("id_photo_positive_url")
    val idPhotoPositiveUrl: String? = null,         // 申请者的证件正面照片url
    @SerializedName("id_photo_back_url")
    val idPhotoBackUrl: String? = null,             // 申请者的证件背面照片url
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
    val tokenValue: Int,                            // 稳定币价值（单位法币种类）
    @SerializedName("token_id")
    val tokenIdx: Long? = null,                     // 稳定币索引
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

    @SerializedName("failed_reason")
    val unapprovedReason: String? = null,           // 州长董事长未批准时的原因
    @SerializedName("remarks")
    val unapprovedRemarks: String? = null           // 州长董事长未批准选择其它时的备注
) : Parcelable
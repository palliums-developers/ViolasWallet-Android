package com.violas.wallet.repository.http.sso

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.violas.wallet.repository.http.governor.SSOApplicationDetailsDTO
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ApplyForStatusDTO(
    @SerializedName("approval_status")
    val approvalStatus: Int,
    @SerializedName("id")
    val applicationId: String = "",                     // 申请ID
    val amount: Long = -1,
    @SerializedName("token_name")
    val tokenName: String = "",
    @SerializedName("module_address")
    //val token_address: String?,
    val tokenIdx: Long = -1,
    @SerializedName("application_date")
    val applicationDate: Long = 0,                      // 申请日期
    @SerializedName("expiration_date")
    val expirationDate: Long = 0                        // 申请失效日期
) : Parcelable {

    companion object {

        fun newInstance(applicationDetails: SSOApplicationDetailsDTO): ApplyForStatusDTO {
            return ApplyForStatusDTO(
                approvalStatus = applicationDetails.applicationStatus,
                applicationId = applicationDetails.applicationId,
                tokenName = applicationDetails.tokenName,
                applicationDate = applicationDetails.applicationDate,
                expirationDate = applicationDetails.expirationDate
            )
        }
    }
}

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
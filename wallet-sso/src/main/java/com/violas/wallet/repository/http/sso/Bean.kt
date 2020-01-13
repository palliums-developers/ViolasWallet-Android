package com.violas.wallet.repository.http.sso

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class ApplyForStatusDTO(
    val amount: Long,
    val approval_status: Int,
    val token_name: String,
    @SerializedName("module_address")
    val token_address: String?
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
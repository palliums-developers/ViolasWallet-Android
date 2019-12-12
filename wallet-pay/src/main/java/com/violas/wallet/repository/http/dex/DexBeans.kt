package com.violas.wallet.repository.http.dex

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 2019-12-05 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

open class Response<T> : ApiResponse {

    @SerializedName(value = "code")
    var errorCode: Int = 0
        get() {
            return if (errorMsg.isNullOrEmpty()) 200 else field
        }

    @SerializedName(value = "error")
    var errorMsg: String? = null

    @SerializedName(value = "data", alternate = ["orders"])
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 200
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }
}

class ListResponse<T> : Response<List<T>>()

data class DexOrderDTO(
    val id: String,
    val user: String,
    val state: String,
    val tokenGive: String,
    val amountGive: String,
    val tokenGet: String,
    val amountGet: String,
    val amountFilled: String,
    val version: Long,
    @SerializedName(value = "update_version")
    val updateVersion: Long,
    var date: Long,
    @SerializedName(value = "update_date")
    val updateDate: Long
) : Parcelable {

    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readString()!!,
        source.readLong(),
        source.readLong(),
        source.readLong(),
        source.readLong()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(id)
        writeString(user)
        writeString(state)
        writeString(tokenGive)
        writeString(amountGive)
        writeString(tokenGet)
        writeString(amountGet)
        writeString(amountFilled)
        writeLong(version)
        writeLong(updateVersion)
        writeLong(date)
        writeLong(updateDate)
    }

    companion object CREATOR : Parcelable.Creator<DexOrderDTO> {
        override fun createFromParcel(source: Parcel): DexOrderDTO = DexOrderDTO(source)
        override fun newArray(size: Int): Array<DexOrderDTO?> = arrayOfNulls(size)
    }
}

data class DexTokenPriceDTO(
    @SerializedName(value = "addr")
    val address: String,
    val name: String,
    val price: Double
)
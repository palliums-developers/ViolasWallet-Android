package com.violas.walletconnect.models

import android.os.Parcel
import android.os.Parcelable

data class WCPeerMeta(
    val name: String,
    val url: String,
    val description: String? = null,
    val icons: List<String> = listOf("")
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.createStringArrayList() ?: arrayListOf()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(url)
        parcel.writeString(description)
        parcel.writeStringList(icons)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WCPeerMeta> {
        override fun createFromParcel(parcel: Parcel): WCPeerMeta {
            return WCPeerMeta(parcel)
        }

        override fun newArray(size: Int): Array<WCPeerMeta?> {
            return arrayOfNulls(size)
        }
    }
}
package com.violas.wallet.ui.selectCurrency.bean

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class CurrencyBean(
    @SerializedName("country_or_territory")
    var countryOrTerritory: String = "",
    @SerializedName("currency")
    var currency: String = "",
    @SerializedName("flag")
    var flag: String = "",
    @SerializedName("indicator")
    var indicator: String = "",
    @SerializedName("exchange")
    var exchange: Float = 0.0F
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readFloat()
    ) {
    }

    fun getNameFirst() = countryOrTerritory[0]
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(countryOrTerritory)
        parcel.writeString(currency)
        parcel.writeString(flag)
        parcel.writeString(indicator)
        parcel.writeFloat(exchange)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CurrencyBean> {
        override fun createFromParcel(parcel: Parcel): CurrencyBean {
            return CurrencyBean(parcel)
        }

        override fun newArray(size: Int): Array<CurrencyBean?> {
            return arrayOfNulls(size)
        }
    }
}
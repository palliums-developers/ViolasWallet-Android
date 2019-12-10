package com.violas.wallet.ui.dexOrder

import android.os.Parcel
import android.os.Parcelable
import com.violas.wallet.repository.http.dex.DexOrderDTO

/**
 * Created by elephant on 2019-12-10 11:03.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
data class DexOrderVO(
    val dexOrderDTO: DexOrderDTO,
    val giveTokenName: String,
    val giveTokenPrice: Double,
    val getTokenName: String,
    val getTokenPrice: Double,
    var revokedFlag: Boolean = false
) : Parcelable {

    fun isFinished(): Boolean {
        return dexOrderDTO.state == "FILLED" || dexOrderDTO.state == "CANCELED"
    }

    fun isCanceled(): Boolean {
        return dexOrderDTO.state == "CANCELED"
    }

    fun isOpen(): Boolean {
        return dexOrderDTO.state == "OPEN"
    }

    constructor(source: Parcel) : this(
        source.readParcelable<DexOrderDTO>(DexOrderDTO::class.java.classLoader)!!,
        source.readString()!!,
        source.readDouble(),
        source.readString()!!,
        source.readDouble()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(dexOrderDTO, 0)
        writeString(giveTokenName)
        writeDouble(giveTokenPrice)
        writeString(getTokenName)
        writeDouble(getTokenPrice)
    }

    companion object CREATOR : Parcelable.Creator<DexOrderVO> {
        override fun createFromParcel(source: Parcel): DexOrderVO = DexOrderVO(source)
        override fun newArray(size: Int): Array<DexOrderVO?> = arrayOfNulls(size)
    }
}
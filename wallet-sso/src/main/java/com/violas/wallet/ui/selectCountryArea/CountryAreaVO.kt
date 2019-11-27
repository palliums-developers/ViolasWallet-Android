package com.violas.wallet.ui.selectCountryArea

import android.os.Parcel
import android.os.Parcelable
import com.palliums.widget.groupList.GroupListLayout

/**
 * Created by elephant on 2019-11-27 16:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 国家地区VO
 */
data class CountryAreaVO(
    val areaCode: String,
    val countryName: String
) : GroupListLayout.ItemData, Parcelable {

    private var groupName: String? = null

    override fun getGroupName(): String? {
        return groupName
    }

    override fun setGroupName(groupName: String) {
        this.groupName = groupName
    }

    override fun compareTo(other: String): Int {
        return 0
    }

    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readString()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(areaCode)
        writeString(countryName)
    }

    companion object CREATOR : Parcelable.Creator<CountryAreaVO> {
        override fun createFromParcel(source: Parcel): CountryAreaVO = CountryAreaVO(source)
        override fun newArray(size: Int): Array<CountryAreaVO?> = arrayOfNulls(size)
    }
}
package com.violas.wallet.ui.selectCountryArea

import android.os.Parcelable
import com.palliums.widget.groupList.GroupListLayout
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2019-11-27 16:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 国家地区VO
 */

@Parcelize
data class CountryAreaVO(
    val areaCode: String,
    val countryName: String,
    val countryCode: String
) : GroupListLayout.ItemData, Parcelable {

    @IgnoredOnParcel
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
}
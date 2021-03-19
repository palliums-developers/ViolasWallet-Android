package com.violas.wallet.biz.bean

import android.os.Parcelable
import com.violas.wallet.common.CURRENCY_DEFAULT_ADDRESS
import kotlinx.android.parcel.Parcelize

/**
 * Diem 标准 Currency
 */
@Parcelize
data class DiemCurrency(
    var module: String,
    var name: String = module,
    var address: String = CURRENCY_DEFAULT_ADDRESS
) : Parcelable

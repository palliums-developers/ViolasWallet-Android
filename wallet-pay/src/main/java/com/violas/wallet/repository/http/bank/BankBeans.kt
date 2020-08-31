package com.violas.wallet.repository.http.bank

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2020/8/24 11:23.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class UserBankInfoDTO(
    @SerializedName("amount")
    var totalDeposit: String,
    @SerializedName("borrow")
    var totalBorrowable: String,
    @SerializedName("total")
    var totalEarnings: String,
    @SerializedName("yesterday")
    var yesterdayEarnings: String,
    @SerializedName("deposits")
    val depositProducts: List<BankProductSummaryDTO>?,
    @SerializedName("borrows")
    val borrowingProducts: List<BankProductSummaryDTO>?
)

@Keep
@Parcelize
data class BankProductSummaryDTO(
    @SerializedName("id")
    val productId: String,
    @SerializedName("name")
    val productName: String,
    @SerializedName("intro")
    val productDesc: String,
    @SerializedName("logo")
    val productLogo: String,
    @SerializedName("rate")
    val productRate: String,        // 存款收益率，或借款借贷率
    @SerializedName("name")
    val tokenName: String,
    @SerializedName("name")
    val tokenModule: String,
    @SerializedName("id")
    val tokenAddress: String = "00000000000000000000000000000000"
) : Parcelable

@Keep
data class CurrDepositDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String = "00000000000000000000000000000000",
    val coinLogo: String,
    val principal: String,          // 本金
    val totalEarnings: String,      // 累计收益
    val depositYield: String        // 存款收益率
)

@Keep
@Parcelize
data class CurrBorrowingDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String = "00000000000000000000000000000000",
    val coinLogo: String,
    val borrowed: String            // 借款金额
) : Parcelable

@Keep
data class DepositRecordDTO(
    val coinName: String,
    val coinLogo: String,
    val amount: String,
    val time: Long,
    val state: Int
)

@Keep
data class BorrowingRecordDTO(
    val coinName: String,
    val coinLogo: String,
    val amount: String,
    val time: Long,
    val state: Int
)

@Keep
data class BorrowingDetailDTO(
    val amount: String,
    val time: Long,
    val state: Int
)

@Keep
data class RepaymentDetailDTO(
    val amount: String,
    val time: Long,
    val state: Int
)

@Keep
data class LiquidationDetailDTO(
    val liquidateCoin: String,
    val liquidateAmount: String,
    val deductCoin: String,
    val deductAmount: String,
    val time: Long,
    val state: Int
)
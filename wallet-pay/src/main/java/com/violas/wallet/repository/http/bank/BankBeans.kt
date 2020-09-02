package com.violas.wallet.repository.http.bank

import android.icu.util.Currency
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

//region /1.0/violas/bank/account/info DTO
@Keep
data class AccountInfoDTO(
    @SerializedName("amount")
    var totalDeposit: String,
    @SerializedName("borrow")
    var totalBorrowable: String,
    @SerializedName("total")
    var totalEarnings: String,
    @SerializedName("yesterday")
    var yesterdayEarnings: String
)
//endregion

//region /1.0/violas/bank/product/deposit DTO
@Keep
data class DepositProductSummaryDTO(
    @SerializedName("id")
    val productId: String,
    @SerializedName("name")
    val productName: String,
    @SerializedName("desc")
    val productDesc: String,
    @SerializedName("logo")
    val productLogo: String,
    @SerializedName("rate")
    val depositYield: String,           // 收益率
    @SerializedName("token_module")
    val tokenModule: String
)
//endregion

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

//region /1.0/violas/bank/deposit/info DTO
@Keep
data class DepositProductDetailsDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("intor")
    val intor: List<IntorDTO>,
    @SerializedName("minimum_amount")
    val minimumAmount: Long,
    @SerializedName("minimum_amount")
    val minimumStep: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("pledge_rate")
    val pledgeRate: Double,
    @SerializedName("question")
    val question: List<QuestionDTO>,
    @SerializedName("quota_limit")
    val quotaLimit: Long,
    @SerializedName("quota_used")
    val quotaUsed: Long,
    @SerializedName("rate")
    val rate: Double,
    @SerializedName("token_address")
    val tokenAddress: String,
    @SerializedName("token_module")
    val tokenModule: String,
    @SerializedName("token_name")
    val tokenName: String,
    @SerializedName("token_show_name")
    val tokenShowName: String
)

@Keep
data class IntorDTO(
    @SerializedName("text")
    val text: String,
    @SerializedName("tital")
    val tital: String
)

@Keep
data class QuestionDTO(
    @SerializedName("text")
    val text: String,
    @SerializedName("tital")
    val tital: String
)
//endregion


//region /1.0/violas/bank/deposit/orders DTO
@Keep
data class DepositInfoDTO(
    @SerializedName("id")
    val productId: String,
    @SerializedName("token_show_name")
    val productName: String,
    @SerializedName("logo")
    val productLogo: String,
    @SerializedName("rate")
    val depositYield: String,           // 收益率

    @SerializedName("principal")
    val principal: String,              // 本金
    @SerializedName("earnings")
    val totalEarnings: String,          // 累计收益
    @SerializedName("available_quantity")
    val availableAmount: String,        // 可用金额(本金 - 被清算)
    @SerializedName("status")
    val status: Int,

    @SerializedName("token_name")
    val coinName: String,
    @SerializedName("currency")
    val coinModule: String,
    @SerializedName("token_address")
    val coinAddress: String = "00000000000000000000000000000000"
)
//endregion

//region /1.0/violas/bank/deposit/order/list DTO
@Keep
data class DepositRecordDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("currency")
    val coinName: String,
    @SerializedName("logo")
    val coinLogo: String,
    @SerializedName("value")
    val amount: String,
    @SerializedName("date")
    val time: Long,
    @SerializedName("status")
    val state: Int
)
//endregion

//region /1.0/violas/bank/product/borrow DTO
@Keep
data class BorrowingProductSummaryDTO(
    @SerializedName("id")
    val productId: String,
    @SerializedName("name")
    val productName: String,
    @SerializedName("desc")
    val productDesc: String,
    @SerializedName("logo")
    val productLogo: String,
    @SerializedName("rate")
    val borrowingRate: String,          // 借贷率
    @SerializedName("token_module")
    val tokenModule: String
)
//endregion

//region /1.0/violas/bank/borrow/info DTO
@Keep
data class BorrowProductDetailsDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("intor")
    val intor: List<IntorDTO>,
    @SerializedName("minimum_amount")
    val minimumAmount: Long,
    @SerializedName("minimum_step")
    val minimumStep: Long = 1000,
    @SerializedName("name")
    val name: String,
    @SerializedName("pledge_rate")
    val pledgeRate: Double,
    @SerializedName("question")
    val question: List<QuestionDTO>,
    @SerializedName("quota_limit")
    val quotaLimit: Long,
    @SerializedName("quota_used")
    val quotaUsed: Long,
    @SerializedName("rate")
    val rate: Double,
    @SerializedName("token_address")
    val tokenAddress: String,
    @SerializedName("token_module")
    val tokenModule: String,
    @SerializedName("token_name")
    val tokenName: String,
    @SerializedName("token_show_name")
    val tokenShowName: String
)
//endregion

//region /1.0/violas/bank/borrow/orders DTO
@Keep
@Parcelize
data class BorrowingInfoDTO(
    @SerializedName("id")
    val productId: String,
    @SerializedName("name")
    val productName: String,
    @SerializedName("logo")
    val productLogo: String,
    @SerializedName("amount")
    val borrowedAmount: String          // 已借贷金额
) : Parcelable
//endregion

//region /1.0/violas/bank/borrow/order/list DTO

@Keep
data class BorrowingRecordDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("currency")
    val coinName: String,
    @SerializedName("logo")
    val coinLogo: String,
    @SerializedName("value")
    val amount: String,
    @SerializedName("date")
    val time: Long,
    @SerializedName("status")
    val state: Int
)
//endregion

//region /1.0/violas/bank/borrow/order/detail DTO
@Keep
data class BorrowOrderDetailDTO(
    @SerializedName("balance")
    val balance: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("list")
    val list: List<BorrowRecordDTO>,
    @SerializedName("name")
    val name: String,
    @SerializedName("rate")
    val rate: Double,
    @SerializedName("token_address")
    val tokenAddress: String,
    @SerializedName("token_module")
    val tokenModule: String,
    @SerializedName("token_name")
    val tokenName: String,
    @SerializedName("token_show_name")
    val tokenShowName: String
)

@Keep
data class BorrowRecordDTO(
    @SerializedName("cleared")
    val cleared: Int,
    @SerializedName("date")
    val date: Int,
    @SerializedName("deductioned")
    val deductioned: Int,
    @SerializedName("status")
    val status: Int
)
//endregion
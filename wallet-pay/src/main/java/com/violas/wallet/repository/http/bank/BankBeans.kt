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

//region /1.0/violas/bank/account/info DTO
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
//endregion

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

//region /1.0/violas/bank/deposit/info DTO
@Keep
data class DepositProductDetailsDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("intor")
    val intor: List<IntorDTO>,
    @SerializedName("minimum_amount")
    val minimumAmount: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("pledge_rate")
    val pledgeRate: Int,
    @SerializedName("question")
    val question: List<QuestionDTO>,
    @SerializedName("quota_limit")
    val quotaLimit: Int,
    @SerializedName("quota_used")
    val quotaUsed: Int,
    @SerializedName("rate")
    val rate: Int,
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
data class DepositOrderInfoDTO(
    @SerializedName("available_quantity")
    val availableQuantity: Int,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("earnings")
    val earnings: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("principal")
    val principal: Int,
    @SerializedName("rate")
    val rate: Double,
    @SerializedName("status")
    val status: Int,
    @SerializedName("token_address")
    val tokenAddress: String,
    @SerializedName("token_name")
    val tokenName: String,
    @SerializedName("token_show_name")
    val tokenShowName: String
)
//endregion

//region /1.0/violas/bank/deposit/order/list DTO
data class DepositOrderDTO(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("date")
    val date: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("value")
    val value: Double
)
//endregion

//region /1.0/violas/bank/borrow/info DTO
data class BorrowProductDetailsDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("intor")
    val intor: List<IntorDTO>,
    @SerializedName("minimum_amount")
    val minimumAmount: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("pledge_rate")
    val pledgeRate: Int,
    @SerializedName("question")
    val question: List<QuestionDTO>,
    @SerializedName("quota_limit")
    val quotaLimit: Int,
    @SerializedName("quota_used")
    val quotaUsed: Int,
    @SerializedName("rate")
    val rate: Int,
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
data class BorrowOrderInfoDTO(
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("name")
    val name: String
)
//endregion

//region /1.0/violas/bank/borrow/order/list DTO
data class BorrowOrderDTO(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("date")
    val date: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("value")
    val value: Double
)
//endregion

//region /1.0/violas/bank/borrow/order/detail DTO
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
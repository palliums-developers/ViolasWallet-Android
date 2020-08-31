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
data class CurrDepositDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String,
    val coinLogo: String,
    val principal: String,           // 本金
    val totalEarnings: String,       // 累计收益
    val sevenDayAnnualYield: String  // 7日年化收益率
)

@Keep
@Parcelize
data class CurrBorrowingDTO(
    val coinName: String,
    val coinModule: String,
    val coinAddress: String,
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

//region /1.0/violas/bank/account/info DTO
@Keep
data class AccountInfoDTO(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("borrow")
    val borrow: Double,
    @SerializedName("borrows")
    val borrows: List<BorrowDTO>,
    @SerializedName("deposits")
    val deposits: List<DepositDTO>,
    @SerializedName("total")
    val total: Double,
    @SerializedName("yesterday")
    val yesterday: Double
)

@Keep
data class BorrowDTO(
    @SerializedName("desc")
    val desc: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("rate")
    val rate: Double
)

@Keep
data class DepositDTO(
    @SerializedName("desc")
    val desc: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("rate")
    val rate: Double
)
//endregion

//region /1.0/violas/bank/deposit/info DTO
@Keep
data class DepositInfo(
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
data class BorrowInfoDTO(
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
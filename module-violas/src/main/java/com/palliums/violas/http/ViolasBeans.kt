package com.palliums.violas.http

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 2019-11-11 15:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas bean
 */
open class Response<T> : ApiResponse {

    @SerializedName(value = "code")
    var errorCode: Int = 0

    @SerializedName(value = "message")
    var errorMsg: String? = null

    @SerializedName(value = "data")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 2000
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }

    override fun getResponseData(): Any? {
        return data
    }
}

@Keep
class ListResponse<T> : Response<List<T>>()

@Keep
data class CurrencysDTO(
    val currencies: List<CurrencyDTO>
)

@Keep
data class CurrencyDTO(
    val address: String,
    val module: String,
    val name: String,
    @SerializedName(value = "show_icon")
    val showLogo: String,
    @SerializedName(value = "show_name")
    val showName: String
)

data class TransactionRecordDTO(
    val sender: String,
    val receiver: String?,
    val amount: String,
    val currency: String,
    val gas: String,
    @SerializedName(value = "gas_currency")
    val gasCurrency: String,
    val expiration_time: Long,
    val sequence_number: Long,
    val version: Long,
    val type: Int,
    val status: Int
)

data class BalanceDTO(
    var address: String = "",
    var balance: Long = 0
)

data class SignedTxnDTO(
    var signedtxn: String = ""
)

data class LoginWebDTO(
    @SerializedName("type")
    val loginType: Int,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("wallets")
    val walletList: List<WalletAccountDTO>
)

data class WalletAccountDTO(
    @SerializedName("identity")
    val walletType: Int,
    @SerializedName("type")
    val coinType: String,
    @SerializedName("name")
    val walletName: String,
    @SerializedName("address")
    val walletAddress: String
)

data class AccountStateDTO(
    @SerializedName("authentication_key")
    val authenticationKey: String?,
    @SerializedName("balance")
    val balance: Long?,
//    val balance: AccountBalance?,
    @SerializedName("sequence_number")
    val sequenceNumber: Long,
    @SerializedName("sent_events_key")
    val sentEventsKey: String?,
    @SerializedName("received_events_key")
    val receivedEventsKey: String?,
    @SerializedName("delegated_withdrawal_capability")
    val delegatedWithdrawalCapability: Boolean?,
    @SerializedName("delegated_key_rotation_capability")
    val delegatedKeyRotationCapability: Boolean?
)

@Keep
data class AccountBalance(
    val amount: Long,
    val currency: String
)

@Keep
data class FiatBalanceDTO(
    val name: String,
    val rate: Double
)

@Keep
data class MarketCurrenciesDTO(
    @SerializedName(value = "btc")
    val bitcoinCurrencies: List<MarketCurrencyDTO>,
    @SerializedName(value = "libra")
    val libraCurrencies: List<MarketCurrencyDTO>,
    @SerializedName(value = "violas")
    val violasCurrencies: List<MarketCurrencyDTO>
)

@Keep
data class MarketCurrencyDTO(
    val address: String,
    val module: String,
    val name: String,
    @SerializedName(value = "show_name")
    val displayName: String,
    @SerializedName(value = "icon")
    val logo: String,
    @SerializedName(value = "index")
    val marketIndex: Int
)

@Keep
data class MarketSwapRecordDTO(
    @SerializedName(value = "input_name")
    val fromName: String,
    @SerializedName(value = "input_amount")
    val fromAmount: String,
    @SerializedName(value = "output_name")
    val toName: String,
    @SerializedName(value = "output_amount")
    val toAmount: String,
    val version: Long,
    val date: Long,
    val status: Int
)

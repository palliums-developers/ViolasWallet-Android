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

    override fun isSuccess(): Boolean {
        return errorCode == 2000
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any? {
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
    @SerializedName(value = "sender")
    val sender: String = "",
    @SerializedName(value = "receiver")
    val receiver: String? = null,
    @SerializedName(value = "amount")
    val amount: String ? = null,
    @SerializedName(value = "currency")
    val currency: String = "",
    @SerializedName(value = "gas")
    val gas: String = "",
    @SerializedName(value = "gas_currency")
    val gasCurrency: String = "",
    @SerializedName(value = "expiration_time")
    val expirationTime: Long = 0,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long?,
    @SerializedName(value = "sequence_number")
    val sequence_number: Long = 0,
    @SerializedName(value = "version")
    val version: Long = 0,
    @SerializedName(value = "type")
    val type: String = "",
    @SerializedName(value = "status")
    val status: String = ""
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
data class FiatRateDTO(
    val name: String,
    val rate: Double
)
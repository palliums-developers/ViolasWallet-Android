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
    val amount: String,
    val gas: String,
    val receiver: String?,
    val receiver_module: String?,
    val sender: String,
    val sender_module: String?,
    val module_name: String?,
    val expiration_time: Long,
    val sequence_number: Long,
    /*
     * 0: write_set
     * 1: mint（平台币铸币）
     * 2: peer_to_peer_transfer（平台币转账）
     * 3: create_account
     * 4: rotate_authentication_key
     * 5: violas_withdrawal
     * 6: violas_order
     * 7: violas_mint（稳定币铸币）
     * 8: violas_owner_init
     * 9: violas_init（publish稳定币）
     * 10: violas_pick
     * 11: violas_module
     * 12: violas_peer_to_peer_transfer（普通稳定币转账）
     * 13: violas_peer_to_peer_transfer_with_data （交易所稳定币转账）
     */
    val type: Int,
    val version: Long
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

data class AccountBalance(
    val amount: Long,
    val currency: String
)
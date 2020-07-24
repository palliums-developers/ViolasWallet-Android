package com.palliums.violas.http

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

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
data class SwapTrialSTO(
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("fee")
    val fee: Long,
    @SerializedName("path")
    val path: List<Int>,
    @SerializedName("rate")
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
    val name: String,
    val module: String,
    val address: String,
    @SerializedName(value = "show_name")
    val displayName: String,
    @SerializedName(value = "icon")
    val logo: String,
    @SerializedName(value = "index")
    val marketIndex: Int
)

@Keep
@Parcelize
data class MarketSwapRecordDTO(
    @SerializedName(value = "input_name")
    val fromName: String?,
    @SerializedName(value = "input_amount")
    val fromAmount: String?,
    @SerializedName(value = "output_name")
    val toName: String?,
    @SerializedName(value = "output_amount")
    val toAmount: String?,
    val version: Long,
    val date: Long,
    val status: Int
) : Parcelable

@Keep
@Parcelize
data class MarketPoolRecordDTO(
    @SerializedName(value = "coina")
    val coinAName: String?,
    @SerializedName(value = "amounta")
    val coinAAmount: String?,
    @SerializedName(value = "coinb")
    val coinBName: String?,
    @SerializedName(value = "amountb")
    val coinBAmount: String?,
    @SerializedName(value = "token")
    val liquidityAmount: String?,
    @SerializedName(value = "transaction_type")
    val type: String,
    val version: Long,
    val date: Long,
    val status: Int
) : Parcelable {
    companion object {
        const val TYPE_ADD_LIQUIDITY = "ADD_LIQUIDITY"
        const val TYPE_REMOVE_LIQUIDITY = "REMOVE_LIQUIDITY"
    }

    fun isAddLiquidity(): Boolean {
        return type.equals(TYPE_ADD_LIQUIDITY, true)
    }
}

@Keep
data class UserPoolInfoDTO(
    @SerializedName(value = "total_token")
    val liquidityTotalAmount: String,
    @SerializedName(value = "balance")
    val liquidityList: List<PoolLiquidityDTO>?
)

@Keep
data class PoolLiquidityDTO(
    @SerializedName(value = "coin_a")
    val coinA: CoinDTO,
    @SerializedName(value = "coin_b")
    val coinB: CoinDTO,
    @SerializedName(value = "token")
    val amount: BigDecimal
) {
    @Keep
    data class CoinDTO(
        val name: String,
        val module: String,
        @SerializedName(value = "module_address")
        val address: String,
        @SerializedName(value = "index")
        val marketIndex: Int,
        @SerializedName(value = "show_name")
        val displayName: String,
        @SerializedName(value = "value")
        val amount: BigDecimal
    )
}

@Keep
data class AddPoolLiquidityEstimateResultDTO(
    @SerializedName(value = "amount")
    val tokenBAmount: BigDecimal,
    @SerializedName(value = "rate")
    val exchangeRate: BigDecimal
)

@Keep
data class RemovePoolLiquidityEstimateResultDTO(
    @SerializedName(value = "coin_a_name")
    val tokenAName: String,
    @SerializedName(value = "coin_a_value")
    val tokenAAmount: BigDecimal,
    @SerializedName(value = "coin_b_name")
    val tokenBName: String,
    @SerializedName(value = "coin_b_value")
    val tokenBAmount: BigDecimal
)

@Keep
data class PoolLiquidityReserveInfoDTO(
    @SerializedName(value = "liquidity_total_supply")
    val liquidityTotalAmount: BigDecimal,
    @SerializedName(value = "coina")
    val coinA: CoinDTO,
    @SerializedName(value = "coinb")
    val coinB: CoinDTO
) {
    @Keep
    data class CoinDTO(
        @SerializedName(value = "name")
        val module: String,
        @SerializedName(value = "index")
        val marketIndex: Int,
        @SerializedName(value = "value")
        val amount: BigDecimal
    )
}

@Keep
data class MapRelationDTO(
    @SerializedName("chain")
    val chain: String,
    @SerializedName("index")
    val index: Int,
    @SerializedName("map_name")
    val mapName: String,
    @SerializedName("module")
    val module: String,
    @SerializedName("module_address")
    val moduleAddress: String,
    @SerializedName("name")
    val name: String
)

data class MappingPairInfoDTO(
    @SerializedName("input_coin_type")
    val inputCoinType: String,
    @SerializedName("lable")
    val lable: String,
    @SerializedName("receiver_address")
    val receiverAddress: String,
    @SerializedName("to_coin")
    val toCoin: ToCoin
)

data class ToCoin(
    @SerializedName("assets")
    val assets: Assets?,
    @SerializedName("coin_type")
    val coinType: String
)

data class Assets(
    @SerializedName("address")
    val address: String,
    @SerializedName("module")
    val module: String,
    @SerializedName("name")
    val name: String
)
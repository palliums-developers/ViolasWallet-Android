package com.violas.wallet.ui.main.quotes.bean

import com.palliums.utils.toBigDecimal
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

interface IToken {
    fun isNetEnable(): Boolean = false
    fun setNetEnable(enable: Boolean)
    fun isEnable(): Boolean = false
    fun tokenIdx(): Long
    fun tokenName(): String
    fun tokenUnit(): String = tokenName()
    fun tokenPrice(): BigDecimal
}

class ExchangeToken(
    private val address: Long,
    private val name: String,
    private var localEnable: Boolean = false,
    private var remoteEnable: Boolean = false
) : IToken {
    override fun tokenIdx() = address

    override fun tokenName() = name

    override fun tokenPrice() = BigDecimal("0")

    override fun isNetEnable() = remoteEnable

    override fun setNetEnable(enable: Boolean) {
        remoteEnable = enable
    }

    override fun isEnable() = localEnable
}

enum class IOrderType {
    BUY, SELLS
}

enum class IOrderStatus {
    OPEN, FILLED, CANCELED, FILLED_CANCELED
}

interface IOrder {
    fun id(): String
    fun version(): Long
    fun updateVersion(): Long
    fun userAddress(): String
    fun tokenGetSymbol(): String
    fun tokenGet(): String
    fun tokenGetPrice(): BigDecimal
    fun tokenGiveSymbol(): String
    fun tokenGive(): String
    fun tokenGivePrice(): BigDecimal
    fun type(): IOrderType
    fun state(): IOrderStatus
    fun amount(): String
    fun balance(): String
    fun price(): String
    fun setPrice(price: String)
    fun date(): Date
}

class ExchangeOrder(
    private val id: String,
    private val userAddress: String,
    private val version: Long,
    private val updateVersion: Long,
    private val tokenGetSymbol: String,
    private val tokenGet: String,
    private val tokenGetPrice: BigDecimal,
    private val tokenGiveSymbol: String,
    private val tokenGive: String,
    private val tokenGivePrice: BigDecimal,
    private var type: IOrderType,
    private var state: IOrderStatus,
    private var amount: String = "0",
    private var balance: String = "0",
    private var price: String = "0",
    private var date: Date = Date(System.currentTimeMillis())
) : IOrder {
    companion object {
        fun parse(jsonArray: JSONArray, type: IOrderType): List<IOrder> {
            val mOrder = ArrayList<IOrder>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val any = jsonArray.getJSONObject(i)
                mOrder.add(
                    ExchangeOrder(
                        any.getString("id"),
                        any.getString("user").replace("0x", ""),
                        any.getString("version").toLong(),
                        any.getString("update_version").toLong(),
                        any.getString("tokenGetSymbol"),
                        any.getString("tokenGet").replace("0x", ""),
                        BigDecimal(any.getDouble("tokenGetPrice").toString()),
                        any.getString("tokenGiveSymbol"),
                        any.getString("tokenGive").replace("0x", ""),
                        BigDecimal(any.getDouble("tokenGivePrice").toString()),
                        type,
                        when (any.getString("state")) {
                            "OPEN" -> IOrderStatus.OPEN
                            "FILLED" -> IOrderStatus.FILLED
                            "CANCELED" -> IOrderStatus.CANCELED
                            else -> IOrderStatus.FILLED_CANCELED
                        },
                        any.getString("amountGet").toBigDecimal().divide(
                            BigDecimal("1000000"),
                            4,
                            RoundingMode.HALF_UP
                        ).stripTrailingZeros().toPlainString(),
                        any.getString("amountGet").toBigDecimal()
                            .subtract(any.getString("amountFilled").toBigDecimal())
                            .divide(
                                BigDecimal("1000000"),
                                4,
                                RoundingMode.HALF_UP
                            ).stripTrailingZeros().toPlainString(),
                        "",
                        Date(any.getLong("update_date") * 1000)
                    )
                )
            }
            return mOrder
        }
    }

    override fun id() = id

    override fun version() = version

    override fun updateVersion() = updateVersion

    override fun userAddress() = userAddress

    override fun tokenGetSymbol() = tokenGetSymbol

    override fun tokenGiveSymbol() = tokenGiveSymbol

    override fun type() = type

    override fun state() = state

    override fun amount() = amount

    override fun balance() = balance

    override fun price() = price

    override fun date() = date

    override fun tokenGet(): String {
        return tokenGet.replace("0x", "")
    }

    override fun tokenGetPrice(): BigDecimal {
        return tokenGetPrice
    }

    override fun tokenGivePrice(): BigDecimal {
        return tokenGivePrice
    }

    override fun setPrice(price: String) {
        this.price = price
    }

    override fun tokenGive(): String {
        return tokenGive.replace("0x", "")
    }

    override fun toString(): String {
        return "${id} $updateVersion"
    }
}
package com.violas.wallet.ui.main.quotes.bean

import org.json.JSONArray
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

interface IToken {
    fun isNetEnable(): Boolean = false
    fun isEnable(): Boolean = false
    fun tokenAddress(): String
    fun tokenName(): String
    fun tokenUnit(): String = tokenName()
    fun tokenPrice(): BigDecimal
}

class ExchangeToken(
    private val address: String,
    private val name: String,
    private val price: BigDecimal,
    private var localEnable: Boolean = false,
    private var remoteEnable: Boolean = false
) : IToken {
    override fun tokenAddress() = address

    override fun tokenName() = name

    override fun tokenPrice() = price

    override fun isNetEnable() = remoteEnable

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
    fun userAddress(): String
    fun tokenGetSymbol(): String
    fun tokenGet(): String
    fun tokenGiveSymbol(): String
    fun tokenGive(): String
    fun type(): IOrderType
    fun state(): IOrderStatus
    fun amount(): String
    fun price(): String
    fun setPrice(price:String)
    fun date(): Date
}

private var sim = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

class ExchangeOrder(
    private val id: String,
    private val userAddress: String,
    private val version: Long,
    private val tokenGetSymbol: String,
    private val tokenGet: String,
    private val tokenGiveSymbol: String,
    private val tokenGive: String,
    private var type: IOrderType,
    private var state: IOrderStatus,
    private var amount: String = "0",
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
                        any.getString("user"),
                        any.getString("version").toLong(),
                        any.getString("tokenGetSymbol"),
                        any.getString("tokenGet"),
                        any.getString("tokenGiveSymbol"),
                        any.getString("tokenGive"),
                        type,
                        when (any.getString("state")) {
                            "OPEN" -> IOrderStatus.OPEN
                            "FILLED" -> IOrderStatus.FILLED
                            "CANCELED" -> IOrderStatus.CANCELED
                            else -> IOrderStatus.FILLED_CANCELED
                        },
                        any.getString("amountGive"),
                        "",
                        Date(any.getLong("update_date"))
                    )
                )
            }
            return mOrder
        }
    }

    override fun id() = id

    override fun version() = version

    override fun userAddress() = userAddress

    override fun tokenGetSymbol() = tokenGetSymbol

    override fun tokenGiveSymbol() = tokenGiveSymbol

    override fun type() = type

    override fun state() = state

    override fun amount() = amount

    override fun price() = price

    override fun date() = date

    override fun tokenGet(): String {
        return tokenGet.replace("0x", "")
    }

    override fun setPrice(price:String){
        this.price = price
    }

    override fun tokenGive(): String {
        return tokenGive.replace("0x", "")
    }
}
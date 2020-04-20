package com.violas.wallet.repository.socket

import android.util.Log
import com.violas.wallet.common.BaseBizUrl
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor
import com.violas.wallet.ui.main.quotes.bean.ExchangeOrder
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IOrderType
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.Executors

interface Subscriber {
    fun onMarkCall(
        myOrder: List<IOrder>,
        buyOrder: List<IOrder>,
        sellOrder: List<IOrder>,
        rate: BigDecimal
    ) {
    }

    fun onDepthsCall(buyOrder: List<IOrder>, sellOrder: List<IOrder>) {}
    fun onReconnect() {}
}

object ExchangeSocket {
    private val mSubscriber = mutableListOf<Subscriber>()
    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mSocket by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(RequestHeaderInterceptor(false))
//            .addInterceptor(HttpLoggingInterceptor().also {
//                it.level = if (BuildConfig.DEBUG)
//                    HttpLoggingInterceptor.Level.HEADERS
//                else
//                    HttpLoggingInterceptor.Level.NONE
//            })
            .build()

        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
        IO.setDefaultOkHttpCallFactory(okHttpClient)
        val options = IO.Options()
        IO.socket(BaseBizUrl.getViolasDexSocketBaseUrl(), options)
    }

    init {
        /*
         * socket.io 从开始连接，到异常断开连接，再到重新连接的事件流，所以可以在收到reconnect事件时重新订阅事件
         * ===> Socket.connect()
         *  connecting
         *  connected
         * ===> 异常断开连接
         *  error
         *  disconnect
         * ===> 重新连接失败
         *  reconnect_attempt
         *  reconnecting
         *  connect_error
         *  reconnect_error
         * ===> 重新连接成功
         *  reconnect_attempt
         *  reconnecting
         *  reconnect
         *  connected
         */
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.e("ExchangeSocket", "connected")
        }.on("market") { args ->
            mExecutor.submit {
                if (args.isNotEmpty()) {
                    val depthsJsonObject = (args[0] as JSONObject).getJSONObject("depths")
                    LogUtil.e("==market==", (args[0] as JSONObject).toString())
                    val buysOrder =
                        ExchangeOrder.parse(depthsJsonObject.getJSONArray("buys"), IOrderType.BUY)
                    val sellsOrder =
                        ExchangeOrder.parse(
                            depthsJsonObject.getJSONArray("sells"),
                            IOrderType.SELLS
                        )
                    val meOrder = ExchangeOrder.parse(
                        (args[0] as JSONObject).getJSONArray("orders"),
                        IOrderType.BUY
                    )
                    val rate = BigDecimal((args[0] as JSONObject).getDouble("price").toString())
                    mSubscriber.forEach {
                        it.onMarkCall(meOrder, buysOrder, sellsOrder, rate)
                    }
                }
            }
        }.on("depths") { args ->
            mExecutor.submit {
                if (args.isNotEmpty()) {
                    val depthsJsonObject = (args[0] as JSONObject)
                    LogUtil.e("==depths==", depthsJsonObject.toString())
                    val buysOrder =
                        ExchangeOrder.parse(depthsJsonObject.getJSONArray("buys"), IOrderType.BUY)
                    val sellsOrder =
                        ExchangeOrder.parse(
                            depthsJsonObject.getJSONArray("sells"),
                            IOrderType.SELLS
                        )
                    mSubscriber.forEach {
                        it.onDepthsCall(buysOrder, sellsOrder)
                    }
                }
            }
        }.on(Socket.EVENT_DISCONNECT) {
            Log.e("ExchangeSocket", "disconnect")
        }.on(Socket.EVENT_MESSAGE) {
            Log.e("ExchangeSocket", "event message ${it}")
        }.on(Socket.EVENT_RECONNECT) {
            Log.e("ExchangeSocket", Socket.EVENT_RECONNECT)
            // 重新连接后，需要重新订阅事件
            mSubscriber.forEach {
                it.onReconnect()
            }
        }
    }

    fun addSubscriber(subscriber: Subscriber) {
        if (mSubscriber.contains(subscriber)) return
        Log.e("==addSubscriber==", "addSubscriber")
        mSubscriber.add(subscriber)
        checkConnect()
    }

    fun removeSubscriber(subscriber: Subscriber) {
        Log.e("==removeSubscriber==", "removeSubscriber")
        mSubscriber.remove(subscriber)
        checkConnect()
    }

    private fun checkConnect() {
        if (mSubscriber.size > 0) {
            if (!mSocket.connected()) {
                mSocket.connect()
            }
        } else {
            mSocket.disconnect()
        }
    }

    fun unSubscribe(tokenBase: Long?, tokenQuote: Long?) {
        if (tokenBase != null && tokenQuote != null) {
            Log.e(
                "Socket.io",
//                """unsubscribe: {"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote"}"""
                """unsubscribe: {"tokenBase":$tokenBase,"tokenQuote":$tokenQuote}"""
            )
            mSocket.emit(
                "unsubscribe",
//                """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote"}"""
                """{"tokenBase":$tokenBase,"tokenQuote":$tokenQuote}"""
            )
        }
    }

    fun getMark(tokenBase: Long, tokenQuote: Long, userAddress: String) {
        Log.e(
            "Socket.io",
            """getMarket: {"tokenBase":$tokenBase,"tokenQuote":$tokenQuote,"user":"0x$userAddress"}"""
        )
        Log.e(
            "Socket.io",
            """subscribe: {"tokenBase":$tokenBase,"tokenQuote":$tokenQuote}"""
        )
        mSocket.emit(
            "getMarket",
            """{"tokenBase":$tokenBase,"tokenQuote":$tokenQuote ,"user":"0x$userAddress"}"""
//            """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote" ,"user":"0x$userAddress"}"""
//            """{"tokenBase":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095","tokenQuote":"0x4ce68dd6e81b400a4edf4146307b10e5030a372414fd49b1accecc0767753070" ,"user":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095"}"""
        )
        mSocket.emit(
            "subscribe",
            """{"tokenBase":$tokenBase,"tokenQuote":$tokenQuote}"""
//            """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote"}"""
//            """{"tokenBase":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095","tokenQuote":"0x4ce68dd6e81b400a4edf4146307b10e5030a372414fd49b1accecc0767753070"}"""
        )
    }
}

object LogUtil {
    /**
     * 截断输出日志
     * @param msg
     */
    fun e(tag: String?, msg: String?) {
        var msg = msg
        if (tag == null || tag.length == 0
            || msg == null || msg.length == 0
        )
            return

        val segmentSize = 3 * 1024
        val length = msg.length.toLong()
        if (length <= segmentSize) {// 长度小于等于限制直接打印
            Log.e(tag, msg)
        } else {
            while (msg!!.length > segmentSize) {// 循环分段打印日志
                val logContent = msg.substring(0, segmentSize)
                msg = msg.replace(logContent, "")
                Log.e(tag, logContent)
            }
            Log.e(tag, msg)// 打印剩余日志
        }
    }
}
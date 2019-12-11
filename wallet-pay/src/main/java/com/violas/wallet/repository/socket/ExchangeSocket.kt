package com.violas.wallet.repository.socket

import android.util.Log
import com.violas.wallet.ui.main.quotes.bean.ExchangeOrder
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IOrderType
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.concurrent.Executors

interface Subscriber {
    fun onMarkCall(myOrder: List<IOrder>, buyOrder: List<IOrder>, sellOrder: List<IOrder>) {}
    fun onDepthsCall(buyOrder: List<IOrder>, sellOrder: List<IOrder>) {}
}

object ExchangeSocket {
    private val mSubscriber = mutableListOf<Subscriber>()
    private val mSocket = IO.socket("http://192.168.1.253:8181")
    private val mExecutor = Executors.newSingleThreadExecutor()

    init {
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.e("ExchangeSocket", "connected")
        }.on("market") { args ->
            mExecutor.submit {
                if (args.isNotEmpty()) {
                    val depthsJsonObject = (args[0] as JSONObject).getJSONObject("depths")
                    LogUtil.e("==market==", depthsJsonObject.toString())
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
                    mSubscriber.forEach {
                        it.onMarkCall(meOrder, buysOrder, sellsOrder)
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
        }
    }

    fun addSubscriber(subscriber: Subscriber) {
        mSubscriber.add(subscriber)
        checkConnect()
    }

    fun removeSubscriber(subscriber: Subscriber) {
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

    fun getMark(tokenBase: String, tokenQuote: String, userAddress: String) {
        mSocket.emit(
            "getMarket",
//            """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote" ,"user":"0x$userAddress"}"""
            """{"tokenBase":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095","tokenQuote":"0x4ce68dd6e81b400a4edf4146307b10e5030a372414fd49b1accecc0767753070" ,"user":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095"}"""
        )
        mSocket.emit(
            "subscribe",
            //            """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote"}"""
            """{"tokenBase":"0x07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095","tokenQuote":"0x4ce68dd6e81b400a4edf4146307b10e5030a372414fd49b1accecc0767753070"}"""
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
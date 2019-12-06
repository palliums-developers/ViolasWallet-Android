package com.violas.wallet.repository.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

interface Subscriber {
    fun onMarkCall(msg: JSONObject) {}
}

object ExchangeSocket {
    private val mSubscriber = mutableListOf<Subscriber>()
    private val mSocket = IO.socket("http://192.168.1.253:8181")

    init {
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.e("ExchangeSocket", "connected")
        }.on("market") { args ->
            if (args.isNotEmpty()) {
                mSubscriber.forEach {
                    it.onMarkCall(args[0] as JSONObject)
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

    fun getMark(data: String) {
//        mSocket.emit("getMarket", data)
        mSocket.emit(
            "getMarket",
            """{"tokenBase":"0x0f7100fcf2d114ef199575f0651620001d210718c680fbe7568c72d6e0160731","tokenQuote":"0x352ba42b3a2fb66bff15f08ea691b5b87eff0fe6a69b79cda364c4cdf787a0a2" ,"user":"0x8e8f033830c60602ef491d0f850094d72d483e602c9a5df845eac7efc3387a38"}"""
        )
    }
}
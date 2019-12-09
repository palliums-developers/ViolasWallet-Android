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

    fun getMark(tokenBase: String, tokenQuote: String, userAddress: String) {
        mSocket.emit(
            "getMarket",
            """{"tokenBase":"0x$tokenBase","tokenQuote":"0x$tokenQuote" ,"user":"0x$userAddress"}"""
        )
    }
}
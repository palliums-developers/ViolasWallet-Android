package com.violas.wallet.biz.btc.outputScript

import com.quincysx.crypto.bitcoin.BitcoinOutputStream
import com.quincysx.crypto.bitcoin.script.Script
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ViolasOutputScript {
    companion object {
        const val OP_VER: Int = 0x0000
        val OP_TYPE_START: ByteArray = byteArrayOf(0x30, 0x00)
        val OP_TYPE_END: ByteArray = byteArrayOf(0x30, 0x01)
        val TYPE_CANCEL: ByteArray = byteArrayOf(0x30, 0x02)
    }

    /**
     * 创建跨链兑换交易
     * @param address 接收地址
     * @param vtokenAddress Token 地址
     */
    fun requestExchange(
        address: ByteArray,
        vtokenAddress: ByteArray,
        sequence: Long = System.currentTimeMillis()
    ): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        dataStream.writeInt16(OP_VER)
        dataStream.write(OP_TYPE_START)
        dataStream.write(address)
        dataStream.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array())
        dataStream.write(vtokenAddress)

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }

    fun cancelExchange(address: ByteArray, sequence: Long = System.currentTimeMillis()): Script {
        val dataStream = BitcoinOutputStream()
        dataStream.write("violas".toByteArray())
        dataStream.writeInt16(OP_VER)
        dataStream.write(TYPE_CANCEL)
        dataStream.write(address)
        dataStream.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(sequence).array())

        val scriptStream = BitcoinOutputStream()
        scriptStream.write(Script.OP_RETURN.toInt())
        Script.writeBytes(dataStream.toByteArray(), scriptStream)
        return Script(scriptStream.toByteArray())
    }
}
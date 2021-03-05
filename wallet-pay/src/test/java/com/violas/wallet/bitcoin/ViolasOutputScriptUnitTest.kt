package com.violas.wallet.bitcoin

import com.quincysx.crypto.bitcoin.BitcoinOutputStream
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ViolasOutputScriptUnitTest {
    @Test
    fun request_exchange_test() {
        val violasOutputScript = ViolasOutputScript()
        val requestExchange = violasOutputScript.requestExchange(
            "f086b6a2348ac502c708ac41d06fe824c91806cabcd5b2b5fa25ae1c50bed3c6".hexToBytes(),
            "cd0476e85ecc5fa71b61d84b9cf2f7fd524689a4f870c46d6a5d901b5ac1fdb2".hexToBytes(),
            20200113201
        )
        Assert.assertEquals(
            requestExchange.bytes.toHex(),
            "6a4c5276696f6c617300003000f086b6a2348ac502c708ac41d06fe824c91806cabcd5b2b5fa25ae1c50bed3c600000004b4054431cd0476e85ecc5fa71b61d84b9cf2f7fd524689a4f870c46d6a5d901b5ac1fdb2"
        )
    }

    @Test
    fun cancel_exchange_test() {
        val violasOutputScript = ViolasOutputScript()
        val requestExchange = violasOutputScript.cancelExchange(
            "f086b6a2348ac502c708ac41d06fe824c91806cabcd5b2b5fa25ae1c50bed3c6".hexToBytes(),
            20200113201
        )
        Assert.assertEquals(
            requestExchange.bytes.toHex(),
            "6a3276696f6c617300003002f086b6a2348ac502c708ac41d06fe824c91806cabcd5b2b5fa25ae1c50bed3c600000004b4054431"
        )
    }

    @Test
    fun bitcoinOutputStreamTest() {
        val value = 4

        val outputStream = BitcoinOutputStream()
        outputStream.writeInt16WithBigEndian(value)
        Assert.assertEquals(
            outputStream.toByteArray().toHex(),
            "0004"
        )

        val byteBuffer =
            ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putChar(value.toChar())
        Assert.assertEquals(
            byteBuffer.array().toHex(),
            "0004"
        )

        val outputStream1 = BitcoinOutputStream()
        outputStream1.writeInt16(value)
        Assert.assertEquals(
            outputStream1.toByteArray().toHex(),
            "0400"
        )

        val byteBuffer1 =
            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(value.toChar())
        Assert.assertEquals(
            byteBuffer1.array().toHex(),
            "0400"
        )
    }
}
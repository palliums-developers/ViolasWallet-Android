package com.violas.wallet

import com.violas.wallet.biz.splitMsg
import org.junit.Assert
import org.junit.Test

class SplitMsgUnitTest {
    @Test
    fun test_split() {
        val qrCodeMsg = splitMsg("bitcoin:sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.coinType, "bitcoin")
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
    }

    @Test
    fun test_split_amount() {
        val qrCodeMsg = splitMsg("bitcoin:sdsdfsdfsdfsdfsdfsdfsdfsdf?amount=1230")
        Assert.assertEquals(qrCodeMsg.coinType, "bitcoin")
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.amount, 1230L)
    }

    @Test
    fun test_split_label() {
        val qrCodeMsg = splitMsg("bitcoin:sdsdfsdfsdfsdfsdfsdfsdfsdf?label=qqqw")
        Assert.assertEquals(qrCodeMsg.coinType, "bitcoin")
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.label, "qqqw")
    }

    @Test
    fun test_split_fll_param() {
        val qrCodeMsg = splitMsg("bitcoin:sdsdfsdfsdfsdfsdfsdfsdfsdf?amount=1230&label=qqqw")
        Assert.assertEquals(qrCodeMsg.coinType, "bitcoin")
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.amount, 1230)
        Assert.assertEquals(qrCodeMsg.label, "qqqw")
    }

    @Test
    fun test_split_error_coinType() {
        val qrCodeMsg = splitMsg(":sdsdfsdfsdfsdfsdfsdfsdfsdf?amount=1230&label=qqqw")
        Assert.assertEquals(qrCodeMsg.coinType, null)
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.label, "qqqw")
    }

    @Test
    fun test_split_address() {
        val qrCodeMsg = splitMsg("sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.coinType, null)
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.label, null)
    }

    @Test
    fun test_split_error_amount() {
        val qrCodeMsg = splitMsg(":sdsdfsdfsdfsdfsdfsdfsdfsdf?amount=a123")
        Assert.assertEquals(qrCodeMsg.coinType, null)
        Assert.assertEquals(qrCodeMsg.address, "sdsdfsdfsdfsdfsdfsdfsdfsdf")
        Assert.assertEquals(qrCodeMsg.amount, 0)
        Assert.assertEquals(qrCodeMsg.label, null)
    }
}
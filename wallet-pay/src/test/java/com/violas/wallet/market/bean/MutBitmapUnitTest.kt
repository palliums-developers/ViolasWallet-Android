package com.violas.wallet.market.bean

import MutBitmap
import org.junit.Assert
import org.junit.Test

class MutBitmapUnitTest {

    @Test
    fun test_bitmap() {
        val bitmap = MutBitmap()

        bitmap.setBit(0)
        bitmap.setBit(1)
        bitmap.setBit(3)
        bitmap.setBit(5)
        bitmap.setBit(8)
        bitmap.setBit(12)
        bitmap.setBit(19)
        bitmap.setBit(32)

        Assert.assertEquals(bitmap.countOnes(), 8)

        Assert.assertEquals(bitmap.getBit(0), true)
        Assert.assertEquals(bitmap.getBit(1), true)

        Assert.assertEquals(bitmap.getBit(19), true)
        Assert.assertEquals(bitmap.getBit(64), false)

        bitmap.setBit(30)
        Assert.assertEquals(bitmap.getBit(30), true)

        Assert.assertEquals(bitmap.countOnes(), 9)
    }
}
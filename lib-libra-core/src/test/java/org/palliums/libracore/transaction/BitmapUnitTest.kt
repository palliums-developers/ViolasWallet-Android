package org.palliums.libracore.transaction

import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.crypto.Bitmap

class BitmapUnitTest {

    @Test
    fun test_bitmap() {
        val bitmap = Bitmap(byteArrayOf(0b0100_0000, 0b11111111.toByte(), 0, 0b1000_0000.toByte()))

        Assert.assertEquals(bitmap.countOnes(), 10)

        Assert.assertEquals(bitmap.getBit(0), false)
        Assert.assertEquals(bitmap.getBit(1), true)

        for (i in 8 until 16)
            Assert.assertEquals(bitmap.getBit(i), true)

        for (i in 16 until 24)
            Assert.assertEquals(bitmap.getBit(i), false)

        Assert.assertEquals(bitmap.getBit(24), true)
        Assert.assertEquals(bitmap.getBit(31), false)
        Assert.assertEquals(bitmap.lastGetBit(), 24)

        bitmap.setBit(30)
        Assert.assertEquals(bitmap.getBit(30), true)
        Assert.assertEquals(bitmap.lastGetBit(), 30)

        Assert.assertEquals(bitmap.countOnes(), 11)
    }
}
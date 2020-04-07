package org.palliums.libracore.transaction

import android.util.Range
import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.wallet.*

class BitmapUnitTest {

    @Test
    fun test_bitmap() {
        val bitmap = byteArrayOf(0b0100_0000, 0b11111111.toByte(), 0, 0b1000_0000.toByte())

        Assert.assertEquals(bitmapCountOnes(bitmap), 10)

        Assert.assertEquals(bitmapGetBit(bitmap, 0), false)
        Assert.assertEquals(bitmapGetBit(bitmap, 1), true)

        for (i in 8 until 16)
            Assert.assertEquals(bitmapGetBit(bitmap, i), true)

        for (i in 16 until 24)
            Assert.assertEquals(bitmapGetBit(bitmap, i), false)

        Assert.assertEquals(bitmapGetBit(bitmap, 24), true)
        Assert.assertEquals(bitmapGetBit(bitmap, 31), false)
        Assert.assertEquals(bitmapLastGetBit(bitmap), 24)

        bitmapSetBit(bitmap, 30)
        Assert.assertEquals(bitmapGetBit(bitmap, 30), true)
        Assert.assertEquals(bitmapLastGetBit(bitmap), 30)

        Assert.assertEquals(bitmapCountOnes(bitmap), 11)
    }
}
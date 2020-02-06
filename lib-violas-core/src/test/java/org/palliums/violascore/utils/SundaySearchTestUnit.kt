package org.palliums.violascore.utils

import org.junit.Assert
import org.junit.Test

class SundaySearchTestUnit {
    @Test
    fun test_sunday_search() {
        val src = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val des = byteArrayOf(5, 6, 7, 8, 9)

        val index = sundaySearch(src, des)
        Assert.assertEquals(index, 4)
    }
}
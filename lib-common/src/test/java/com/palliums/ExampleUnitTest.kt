package com.palliums

import com.palliums.utils.formatDate
import com.palliums.utils.utcToLocal
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun utcTest() {
        val utc = "2015-12-14T05:29:03.427Z"
        val utcToLocal = utcToLocal(utc)
        val local = formatDate(utcToLocal, pattern = "yyyy-MM-dd HH:mm:ss")
        println("utc: $utc to local: $local")
    }
}

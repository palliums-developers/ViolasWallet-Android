package org.palliums.violascore.move

import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals

@RunWith(AndroidJUnit4::class)
class MoveTest {
    @Test
    fun test_move_code() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val moveCode = Move.decode(appContext.assets.open("move/test.json"))
        assertArrayEquals(
            moveCode, byteArrayOf(
                76,
                73,
                66,
                82,
                65,
                86,
                77,
                10,
                1,
                0,
                7,
                1,
                74,
                0,
                0,
                0,
                4,
                0,
                0,
                0,
                3,
                78,
                0,
                0,
                0,
                6,
                0,
                0,
                0,
                12,
                84,
                0,
                0,
                0,
                6,
                0,
                0,
                0,
                13,
                90,
                0,
                0,
                0,
                6,
                0,
                0,
                0,
                5,
                96,
                0,
                0,
                0,
                41,
                0,
                0,
                0,
                4,
                137.toByte(),
                0,
                0,
                0,
                32,
                0,
                0,
                0,
                7,
                169.toByte(),
                0,
                0,
                0,
                15,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                2,
                0,
                1,
                3,
                0,
                2,
                0,
                2,
                4,
                2,
                0,
                3,
                0,
                3,
                2,
                4,
                2,
                6,
                60,
                83,
                69,
                76,
                70,
                62,
                12,
                76,
                105,
                98,
                114,
                97,
                65,
                99,
                99,
                111,
                117,
                110,
                116,
                4,
                109,
                97,
                105,
                110,
                15,
                112,
                97,
                121,
                95,
                102,
                114,
                111,
                109,
                95,
                115,
                101,
                110,
                100,
                101,
                114,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                2,
                1,
                4,
                0,
                12,
                0,
                12,
                1,
                19,
                1,
                0,
                2
            )
        )
    }
}
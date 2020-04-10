package org.palliums.libracore.move

import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.palliums.libracore.serialization.hexToBytes

@RunWith(AndroidJUnit4::class)
class MoveTest {
    @Test
    fun test_move_code() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val moveCode = Move.decode(appContext.assets.open("move/test.mv"))
        assertArrayEquals(
            moveCode, "a11ceb0b010007014600000004000000034a0000000c000000045600000002000000055800000009000000076100000029000000068a00000010000000099a0000001200000000000001010200010101000300010101000203050a020300010900063c53454c463e0c4c696272614163636f756e740f7061795f66726f6d5f73656e646572046d61696e00000000000000000000000000000000010000ffff030005000a000b010a023e0002".hexToBytes()
        )
    }
}
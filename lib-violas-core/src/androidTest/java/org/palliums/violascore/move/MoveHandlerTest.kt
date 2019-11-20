package org.palliums.violascore.move

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.move.Move.violasTokenEncode
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex

@RunWith(AndroidJUnit4::class)
class MoveHandlerTest {
    @Test
    fun test_find_address_index(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val moveCode = Move.decode(appContext.assets.open("move/token_transfer.json"))
        val tokenAddress = "7257c2417e4d1038e1817c8f283ace2e1041b3396cdbb099eb357bbee024d614".hexToBytes()
        val index = Move.findAddressIndex(moveCode,tokenAddress)
        Assert.assertEquals(index,156)
    }

    @Test
    fun test_new_move_code(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val violasTokenEncode = violasTokenEncode(
            appContext.assets.open("move/token_transfer.json"),
            "05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054".hexToBytes()
        )
        Assert.assertEquals("4c49425241564d0a010007014a00000004000000034e000000060000000d54000000060000000e5a0000000600000005600000001c000000047c0000004000000008bc0000000f00000000000101000200010300020002040200030204020300063c53454c463e0644546f6b656e046d61696e087472616e73666572000000000000000000000000000000000000000000000000000000000000000005599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054000100020004000c000c0113010102",violasTokenEncode.toHex())
    }
}
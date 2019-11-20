package org.palliums.violascore.move

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.serialization.hexToBytes

@RunWith(AndroidJUnit4::class)
class MoveHandlerTest {
    @Test
    fun test_find_address_index(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val moveCode = Move.decode(appContext.assets.open("move/token_transfer.json"))
        val tokenAddress = "7257c2417e4d1038e1817c8f283ace2e1041b3396cdbb099eb357bbee024d614".hexToBytes()
        val index = Move.findAddressIndex(moveCode,tokenAddress)
        Assert.assertEquals(index,161)
    }
}
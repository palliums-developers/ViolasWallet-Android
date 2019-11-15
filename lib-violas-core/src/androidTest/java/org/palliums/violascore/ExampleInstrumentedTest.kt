package org.palliums.violascore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.SignedTransaction
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.utils.HexUtils
import org.palliums.violascore.wallet.LibraWallet
import org.palliums.violascore.wallet.WalletConfig

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val joinToString =
            "display paddle crush crowd often friend topple agent entry use host begin".split(" ")
                .toMutableList()

        val account = LibraWallet(WalletConfig(joinToString)).generateAccount(0)

        val moveEncode = Move.violasPublishTokenEncode(
            context.assets.open("move/token_publish.json"),
            "05599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054".hexToBytes()
        )

        Assert.assertEquals("b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89",account.getAddress().toHex())

        val program = TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf()
            )
        )

        val rawTransaction = RawTransaction(
            AccountAddress(HexUtils.fromHex(account.getAddress().toHex())),
            0,
            program,
            140000,
            0,
            0
        )

        val toByteString = rawTransaction.toByteArray()
        Assert.assertEquals("b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89000000000000000002000000c00000004c49425241564d0a010007014a00000004000000034e000000060000000d54000000040000000e5800000002000000055a0000001b00000004750000004000000008b50000000b00000000000101000200010300020000000300063c53454c463e0644546f6b656e046d61696e077075626c697368000000000000000000000000000000000000000000000000000000000000000005599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054000100010002001301000200000000e02202000000000000000000000000000000000000000000",HexUtils.toHex(toByteString))
        println("rawTransaction ${HexUtils.toHex(toByteString)}")
        println("code ${HexUtils.toHex(moveEncode)}")

        val signedTransaction = SignedTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.sign(rawTransaction.toByteArray())
        )

        Assert.assertEquals("b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89000000000000000002000000c00000004c49425241564d0a010007014a00000004000000034e000000060000000d54000000040000000e5800000002000000055a0000001b00000004750000004000000008b50000000b00000000000101000200010300020000000300063c53454c463e0644546f6b656e046d61696e077075626c697368000000000000000000000000000000000000000000000000000000000000000005599ef248e215849cc599f563b4883fc8aff31f1e43dff1e3ebe4de1370e054000100010002001301000200000000e022020000000000000000000000000000000000000000002000000024e236320adcdf04306257212433bbcaa0d8ccc6037cae4440455146c9cf8bf6400000008c39b347d4a6fe22146b23ed28b23df341a5f3eb905bbbd4227f8b94b38d2325add8da52870449d0109efbe2e2615c6e15cbf548e576e2b203911ccddca64407",HexUtils.toHex(signedTransaction.toByteArray()))
//        sendTransaction(
//            rawTransaction,
//            account.keyPair.getPublicKey(),
//            account.keyPair.sign(rawTransaction.toByteArray()),
//            call
//        )
    }
}

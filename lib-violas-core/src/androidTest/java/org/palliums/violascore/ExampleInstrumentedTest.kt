package org.palliums.violascore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.*
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

        val moveEncode = Move.violasTokenEncode(
            context.assets.open("move/token_publish.json"),
            "b9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e".hexToBytes()
        )

        Assert.assertEquals(
            "b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89",
            account.getAddress().toHex()
        )

        val program = TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(lbrStructTag()),
                arrayListOf()
            )
        )

        val rawTransaction = RawTransaction(
            AccountAddress(HexUtils.fromHex(account.getAddress().toHex())),
            0,
            program,
            280000,
            0,
            lbrStructTagType(),
            0
        )

        val toByteString = rawTransaction.toByteArray()
        Assert.assertEquals(
            "b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89000000000000000002000000c50000004c49425241564d0a010007014a00000004000000034e000000060000000d54000000040000000e5800000002000000055a00000020000000047a0000004000000008ba0000000b00000000000101000200010300020000000300063c53454c463e0b56696f6c6173546f6b656e046d61696e077075626c6973680000000000000000000000000000000000000000000000000000000000000000b9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e000100010002001301000200000000c04504000000000000000000000000000000000000000000",
            HexUtils.toHex(toByteString)
        )
        println("rawTransaction ${HexUtils.toHex(toByteString)}")
        println("code ${HexUtils.toHex(moveEncode)}")

        val begin = System.currentTimeMillis()
        val value = account.keyPair.signMessage(rawTransaction.toHashByteArray())
        val end = System.currentTimeMillis()

        println(
            "signed time:${(end - begin) / 1000.toDouble()} s"
        )

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                value
            )
        )

        Assert.assertEquals(
            "b45d3e7e8079eb16cd7111b676f0c32294135e4190261240e3fd7b96fe1b9b89000000000000000002000000c50000004c49425241564d0a010007014a00000004000000034e000000060000000d54000000040000000e5800000002000000055a00000020000000047a0000004000000008ba0000000b00000000000101000200010300020000000300063c53454c463e0b56696f6c6173546f6b656e046d61696e077075626c6973680000000000000000000000000000000000000000000000000000000000000000b9e3266ca9f28103ca7c9bb9e5eb6d0d8c1a9d774a11b384798a3c4784d5411e000100010002001301000200000000c045040000000000000000000000000000000000000000002000000024e236320adcdf04306257212433bbcaa0d8ccc6037cae4440455146c9cf8bf64000000055f75087ecfce74036032b017e24962790bb203b0c1ddf902219431b271a2644d97c7d463426d119e566b249336bc467a5a72bb23db50ea53c4272e4c05c1507",
            HexUtils.toHex(signedTransaction.toByteArray())
        )
//        sendTransaction(
//            rawTransaction,
//            account.keyPair.getPublicKey(),
//            account.keyPair.sign(rawTransaction.toByteArray()),
//            call
//        )
    }
}

package org.palliums.violascore.transfer

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.palliums.violascore.crypto.Ed25519PublicKey
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.crypto.KeyPair

class TransferUnitTest {
    @Test
    fun test_transfer() {
        //

        val account =
            Account(KeyPair.fromSecretKey("ae84a28c5d5956552dd41bbf0ed196affa5bdf25c1b8dd4fa47eb75cab6ce9fa".hexToBytes()))

        val address = "7f4644ae2b51b65bd3c9d414aa853407"
        val amount = 1000L

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val publishTokenPayload = TransactionPayload.optionTransactionPayload(
            appContext, address, amount
        )

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            publishTokenPayload,
            0,
            chainId = 2
        )
        Log.e("===", account.keyPair.getPrivateKey().toHex())
        Log.e("===", account.keyPair.getPublicKey().toHex())

        Log.e(
            "===",
            AuthenticationKey.ed25519(Ed25519PublicKey(account.keyPair.getPublicKey().toByteArray())).toHex()
        )

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        Log.e("===", signedTransaction.toByteArray().toHex())
    }
}
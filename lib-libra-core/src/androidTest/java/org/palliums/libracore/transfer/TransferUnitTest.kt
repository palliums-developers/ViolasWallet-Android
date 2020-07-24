package org.palliums.libracore.transfer

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.*
import org.palliums.libracore.wallet.Account
import org.palliums.libracore.crypto.KeyPair

class TransferUnitTest {
    @Test
    fun test_transfer() {
        //

        val account =
            Account(KeyPair.fromSecretKey("ae84a28c5d5956552dd41bbf0ed196affa5bdf25c1b8dd4fa47eb75cab6ce9fa".hexToBytes()))

        val address = "bfa4166a525870d7187e1b15b397be20000992691edd7918b6b2df893c6f33ab"
        val amount = 0L

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
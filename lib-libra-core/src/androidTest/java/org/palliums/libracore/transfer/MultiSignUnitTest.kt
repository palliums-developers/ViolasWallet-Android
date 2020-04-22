package org.palliums.libracore.transfer

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.crypto.MultiEd25519PrivateKey
import org.palliums.libracore.crypto.MultiEd25519PrivateKeyIndex
import org.palliums.libracore.crypto.MultiEd25519PublicKey
import org.palliums.libracore.crypto.MultiEd25519Signature
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.*
import org.palliums.libracore.wallet.*

class MultiSignUnitTest {

    @Test
    fun test_sign() {

        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()

        println(account1.keyPair.getPrivateKey().toHex())
        println(account2.keyPair.getPrivateKey().toHex())

        val multiEd25519PrivateKey = MultiEd25519PrivateKey(
            arrayListOf(
                account1.keyPair.getPrivateKey(),
                account2.keyPair.getPrivateKey()
            ), 1
        )

        val signMessage = multiEd25519PrivateKey.signMessage("010203".hexToBytes())

        println(signMessage.toByteArray().toHex())
        Assert.assertEquals(
            signMessage.toByteArray().toHex(),
            "c2a4f12f0dd956c1c9c735c48375d378063a995f7f1217cc5345248ddfdbca0f58a5c1bbd4e1109d8111f1775445861ab0b1688281ffd2a408fa5868acd6200a80000000"
        )
    }

    @Test
    fun test_multi_ed25519_bytes() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()

        println(account1.keyPair.getPublicKey().toHex())
        println(account2.keyPair.getPublicKey().toHex())

        val multiEd25519PublicKey = MultiEd25519PublicKey(
            arrayListOf(
                account1.keyPair.getPublicKey(),
                account2.keyPair.getPublicKey()
            ), 1
        )

        val multiAddress =
            AuthenticationKey.multiEd25519(
                multiEd25519PublicKey
            ).getShortAddress().toHex()
        val multiAuthenticationKey = AuthenticationKey.multiEd25519(
            multiEd25519PublicKey
        ).toHex()

        println(multiEd25519PublicKey.toByteArray().toHex())
        Assert.assertEquals(
            multiEd25519PublicKey.toByteArray().toHex(),
            "c413ea446039d0cd07715ddedb8169393e456b03d05ce67d50a4446ba5e067b0005c135145c60db0253e164a6f9fa396ae7e376761538ac55b40747690e757de01"
        )
        println(multiAddress)
        Assert.assertEquals(
            multiAddress,
            "cd35f1a78093554f5dc9c61301f204e4"
        )
        println(multiAuthenticationKey)
        Assert.assertEquals(
            multiAuthenticationKey,
            "7aa0e0507bd766b7a81d250cc6d7d25bcd35f1a78093554f5dc9c61301f204e4"
        )
    }

    @Test
    fun test_tran_multi() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()

        val multiEd25519PrivateKey = MultiEd25519PrivateKey(
            arrayListOf(
                account1.keyPair.getPrivateKey(),
                account2.keyPair.getPrivateKey()
            ), 1
        )

        val multiEd25519PublicKey = MultiEd25519PublicKey(
            arrayListOf(
                account1.keyPair.getPublicKey(),
                account2.keyPair.getPublicKey()
            ), 1
        )

        val senderAddress =
            AuthenticationKey.multiEd25519(
                multiEd25519PublicKey
            ).getShortAddress().toHex()

        val address = "7f4644ae2b51b65bd3c9d414aa853407"
        val amount = (1.02 * 1000000L).toLong()

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val publishTokenPayload = TransactionPayload.optionTransactionPayload(
            appContext, address, amount
        )

        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress,
            publishTokenPayload,
            3
        )

        val signedTransaction =
            SignedTransaction(
                rawTransaction,
                TransactionMultiSignAuthenticator(
                    multiEd25519PublicKey,
                    multiEd25519PrivateKey.signMessage(rawTransaction.toHashByteArray())
                )
            )

        println("signTransaction ${signedTransaction.toByteArray().toHex()}")
    }

    @Test
    fun test_multi_sign_build() {
        val multiEd25519Signature = MultiEd25519Signature.Builder()
            .addSignature(1, "01".hexToBytes())
            .addSignature(5, "05".hexToBytes())
            .addSignature(2, "02".hexToBytes())
            .build()

        Assert.assertEquals(multiEd25519Signature.toByteArray().toHex(), "01020564000000")
    }

    @Test
    fun test_tran_multi1() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account1 = libraWallet.generateAccount(0)
        val account2 = libraWallet.generateAccount(1)
        val account3 = libraWallet.generateAccount(2)

        val multiEd25519PrivateKey = MultiEd25519PrivateKeyIndex(
            arrayListOf(
                Pair(account3.keyPair.getPrivateKey(), 2),
                Pair(account1.keyPair.getPrivateKey(), 0)
            ), 2
        )

//        val multiEd25519PrivateKey = MultiEd25519PrivateKey(
//            arrayListOf(
//                account1.keyPair.getPrivateKey(),
//                account3.keyPair.getPrivateKey()
//                //, Pair(account2.keyPair.getPrivateKey(), 1)
//            ), 2
//        )

        val multiEd25519PublicKey = MultiEd25519PublicKey(
            arrayListOf(
                account1.keyPair.getPublicKey(),
                account2.keyPair.getPublicKey(),
                account3.keyPair.getPublicKey()
            ), 2
        )

        val multiAuthenticationKey = AuthenticationKey.multiEd25519(
            multiEd25519PublicKey
        )
        val senderAddress = multiAuthenticationKey.getShortAddress().toHex()

        println("multi public address : $senderAddress")
        println("multi authenticationKey : ${multiAuthenticationKey.toHex()}")

        val address = "7f4644ae2b51b65bd3c9d414aa853407"
        val amount = (1.00 * 1000000L).toLong()

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val publishTokenPayload = TransactionPayload.optionTransactionPayload(
            appContext, address, amount
        )

        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress,
            publishTokenPayload,
            0
        )

        val signedTransaction =
            SignedTransaction(
                rawTransaction,
                TransactionMultiSignAuthenticator(
                    multiEd25519PublicKey,
                    multiEd25519PrivateKey.signMessage(rawTransaction.toHashByteArray())
                )
            )

        println("signTransaction ${signedTransaction.toByteArray().toHex()}")
    }

    private fun generateMnemonic(): List<String> {
        val mnemonic =
            "school problem vibrant royal invite that never key thunder pizza mesh punch"
//        val mnemonic =
//            "key shoulder focus dish donate inmate move weekend hold regret peanut link"
        val mnemonicWords = mnemonic.split(" ")

        return mnemonicWords
    }
}
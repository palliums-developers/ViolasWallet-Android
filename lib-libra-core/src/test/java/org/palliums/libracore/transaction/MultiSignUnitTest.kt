package org.palliums.libracore.transaction

import android.content.Context
import android.util.Log
import org.junit.Test
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
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

        val toHex = AuthenticationKey.multi_ed25519(multiEd25519PublicKey).getShortAddress().toHex()
        val toHex1 = AuthenticationKey.multi_ed25519(multiEd25519PublicKey).toHex()

        println(multiEd25519PublicKey.toByteArray().toHex())
        println(toHex)
        println(toHex1)
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
            AuthenticationKey.multi_ed25519(multiEd25519PublicKey).getShortAddress().toHex()

        val address = "7f4644ae2b51b65bd3c9d414aa853407"
        val amount = (1.02 * 1000000L).toLong()

        val publishTokenPayload = TransactionPayload.optionTransactionPayload(
            null, address, amount
        )

        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress,
            publishTokenPayload,
            2
        )

        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionMultiSignAuthenticator(
                multiEd25519PublicKey,
                multiEd25519PrivateKey.signMessage(rawTransaction.hashByteArray())
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
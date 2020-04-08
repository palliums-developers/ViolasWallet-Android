package org.palliums.libracore.transaction

import android.util.Range
import org.junit.Assert
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

    private fun generateMnemonic(): List<String> {
        val mnemonic =
            "school problem vibrant royal invite that never key thunder pizza mesh punch"
//        val mnemonic =
//            "key shoulder focus dish donate inmate move weekend hold regret peanut link"
        val mnemonicWords = mnemonic.split(" ")

        return mnemonicWords
    }
}
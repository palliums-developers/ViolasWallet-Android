package org.palliums.libracore.wallet

import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.crypto.Ed25519Signature
import org.palliums.libracore.crypto.KeyFactory
import org.palliums.libracore.crypto.Seed
import org.palliums.libracore.crypto.sha3
import org.palliums.libracore.serialization.toHex
import org.spongycastle.util.encoders.Hex

/**
 * Created by elephant on 2019-09-23 16:07.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class WalletTest {

    @Test
    fun testGenerateSeed() {
        val seed = Seed.fromMnemonic(generateMnemonic(), "LIBRA")
        val seedHexStr = Hex.toHexString(seed.data)
        println()
        println("Wallet seed: $seedHexStr")

        Assert.assertEquals(
            seedHexStr,
            "9fca0ead4ba52cfe1514785ff0a808f575cb72b74e5cb1e0686c991f058874b0"
        )
    }

    @Test
    fun testMasterPrk() {
        val keyFactory = KeyFactory(
            Seed.fromMnemonic(
                generateMnemonic(),
                "LIBRA"
            )
        )
        val masterPrkStr = Hex.toHexString(keyFactory.masterPrk)
        println()
        println("Master Private Key: $masterPrkStr")

        Assert.assertEquals(
            masterPrkStr,
            "e3bffbac970f7eb023287346677051e8426d2c92d85b4b5edc99deac47952aed"
        )
    }

    @Test
    fun testGenerateKey() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()
        val privateKey = account.keyPair.getPrivateKey().toHex()
        val publicKey = account.keyPair.getPublicKey().toHex()
        println()
        println("Private Key: $privateKey")
        println("Public Key: $publicKey")

        Assert.assertEquals(
            privateKey,
            "a65111b2a22a1874f0f532f6b1a0991fbf1fedb9df8ce55a49b3e35be73ff228"
        )
        Assert.assertEquals(
            publicKey,
            "b505dfc25dc69adffa72e6bc507f8cf1ad57ad442ff4bc86f5c2e41b522b8c46"
        )
    }

    @Test
    fun testSign() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()
        val signHexStr = account.keyPair.signMessage(Hex.decode("1234567890")).toHex()
        println()
        println("message sign: $signHexStr")

        Assert.assertEquals(
            signHexStr,
            "0bcb5c73b6919a6074fb4dfdb01d6496e691ac107097da68ff3ffa7a7225a5d425ea1e06c429c5a504ce2a6240c59aa765d813f7f379118b2ece059d1e8c370f"
        )
    }

    @Test
    fun testNewAccount() {
        //        val mnemonic =
//            "school problem vibrant royal invite that never key thunder pizza mesh punch"
        val mnemonic =
            "velvet version sea near truly open blanket exchange leaf cupboard shine poem"
//        val mnemonic =
//            "key shoulder focus dish donate inmate move weekend hold regret peanut link"

        val mnemonicWords = mnemonic.split(" ")

        val libraWallet = LibraWallet(WalletConfig(mnemonicWords))

        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()

        val authenticationKey1 = account1.getAuthenticationKey().toHex()
        val address1 = account1.getAddress().toHex()

        val authenticationKey2 = account2.getAuthenticationKey().toHex()
        val address2 = account2.getAddress().toHex()
        println()
        println("Account 1 private key: ${account1.keyPair.getPrivateKey().toHex()}")
        println("Account 2 private key: ${account2.keyPair.getPrivateKey().toHex()}")
        println()
        println("Account 1 public key: ${account1.keyPair.getPublicKey().toHex()}")
        println("Account 2 public key: ${account2.keyPair.getPublicKey().toHex()}")
        println()
        println("Account 1 AuthenticationKey: $authenticationKey1")
        println("Account 2 AuthenticationKey: $authenticationKey2")
        println()
        println("Account 1 address: $address1")
        println("Account 2 address: $address2")

        Assert.assertEquals(
            address1,
            "7f4644ae2b51b65bd3c9d414aa853407"
        )
        Assert.assertEquals(
            address2,
            "41c30f54f4adfb37d2b16e6f245e8372"
        )
    }

    @Test
    fun testSignSimpleAndVerifySimple() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()

        val data = "session_id_123"
        val signedData = account.keyPair.signMessage(data.toByteArray().sha3())
        println()
        println("data sign simple: ${signedData.toHex()}")

        val result = account.keyPair.verify(signedData, data.toByteArray().sha3())

        Assert.assertEquals(
            result,
            true
        )
    }

    private fun generateMnemonic(): List<String> {
        val mnemonic =
            "velvet version sea near truly open blanket exchange leaf cupboard shine poem"
        val mnemonicWords = mnemonic.split(" ")

        return mnemonicWords
    }
}
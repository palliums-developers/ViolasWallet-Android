package org.palliums.libracore.wallet

import org.junit.Assert
import org.junit.Test
import org.palliums.libracore.crypto.sha3
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.AuthenticationKey
import org.spongycastle.jcajce.provider.digest.SHA3
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
            "338d7f69773dd191683378dcd98897f505f3605d5746039bd9cc5aabf19d392f"
        )
    }

    @Test
    fun testMasterPrk() {
        val keyFactory = KeyFactory(Seed.fromMnemonic(generateMnemonic(), "LIBRA"))
        val masterPrkStr = Hex.toHexString(keyFactory.masterPrk)
        println()
        println("Master Private Key: $masterPrkStr")

        Assert.assertEquals(
            masterPrkStr,
            "a6ebf6ef1032e4a55ce94a13fd73cb7bf727f914e66734fb13ce64dfb5f02444"
        )
    }

    @Test
    fun testGenerateKey() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()
        val privateKey = Hex.toHexString(account.keyPair.getPrivateKey())
        val publicKey = Hex.toHexString(account.keyPair.getPublicKey())
        println()
        println("Private Key: $privateKey")
        println("Public Key: $publicKey")

        Assert.assertEquals(
            privateKey,
            "f3cdd2183629867d6cfa24fb11c58ad515d5a4af014e96c00bb6ba13d3e5f80e"
        )
        Assert.assertEquals(
            publicKey,
            "c413ea446039d0cd07715ddedb8169393e456b03d05ce67d50a4446ba5e067b0"
        )
    }

    @Test
    fun testSign() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()
        val signHexStr = Hex.toHexString(account.keyPair.signMessage(Hex.decode("1234567890")))
        println()
        println("message sign: $signHexStr")

        Assert.assertEquals(
            signHexStr,
            "7233bd1e6ab55c720a8e5e9cfc90a34ab7cbe60580a3721d35bcf3a44d7cab666f10f6eb1bb669c92bbfd017210e2ce336b56a36ca1feefcca78d6718cdec109"
        )
    }

    @Test
    fun testNewAccount() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))

        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()
//        val address1 = account1.getAddress().toHex()
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
            "d040ad00457129ecf5ead5d299627a44"
        )
        Assert.assertEquals(
            address2,
            "5a79e96c3105aabbfb6c5c027a0ef821"
        )
    }

    @Test
    fun testSignSimpleAndVerifySimple() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))
        val account = libraWallet.newAccount()

        val data = "session_id_123"
        val signedData = account.keyPair.signMessage(data.toByteArray().sha3())
        println()
        println("data sign simple: ${Hex.toHexString(signedData)}")

        val result = account.keyPair.verify(data.toByteArray().sha3(), signedData)

        Assert.assertEquals(
            result,
            true
        )
    }

    private fun generateMnemonic(): List<String> {
        //         val mnemonicWords1 = LibraWallet.generateMnemonic()
        //         println()
        //         println("generated mnemonic words: ${mnemonicWords1.joinToString(" ")}")

//        val mnemonic =
//            "school problem vibrant royal invite that never key thunder pizza mesh punch"
        val mnemonic =
            "velvet version sea near truly open blanket exchange leaf cupboard shine poem"
//        val mnemonic =
//            "key shoulder focus dish donate inmate move weekend hold regret peanut link"
        val mnemonicWords = mnemonic.split(" ")

        return mnemonicWords
    }
}
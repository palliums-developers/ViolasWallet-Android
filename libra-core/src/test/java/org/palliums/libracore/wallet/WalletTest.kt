package org.palliums.libracore.wallet

import org.junit.Assert
import org.junit.Test
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
        val signHexStr = Hex.toHexString(account.keyPair.sign(Hex.decode("1234567890")))
        println()
        println("message sign: $signHexStr")

        Assert.assertEquals(
            signHexStr,
            "f8ee0efcdd875e0779e6baa8b07604ec7820d01a9cf7d16771bbe1496cae2a6c30bcbb03176e3fea23e147a2d1740568218bd112e070791138a4ad78190d4a0d"
        )
    }

    @Test
    fun testNewAccount() {
        val libraWallet = LibraWallet(WalletConfig(generateMnemonic()))

        val account1 = libraWallet.newAccount()
        val account2 = libraWallet.newAccount()
        val address1 = account1.getAddress().toHex()
        val address2 = account2.getAddress().toHex()
        println()
        println("Account 1 address: $address1")
        println("Account 2 address: $address2")

        Assert.assertEquals(
            address1,
            "3bc6a5ae944984ac296b72b69f5fbbcbfd4088c446e0958f400517462106154d"
        )
        Assert.assertEquals(
            address2,
            "46b4cf163e34090dc8e482a5def71d259fc6eaf2f59884abddc38f05f30f715a"
        )
    }

    private fun generateMnemonic(): List<String> {
        //         val mnemonicWords1 = LibraWallet.generateMnemonic()
        //         println()
        //         println("generated mnemonic words: ${mnemonicWords1.joinToString(" ")}")

        val mnemonic =
            "school problem vibrant royal invite that never key thunder pizza mesh punch"
        val mnemonicWords = mnemonic.split(" ")

        return mnemonicWords
    }
}
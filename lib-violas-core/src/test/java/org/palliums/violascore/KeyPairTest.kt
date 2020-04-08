package org.palliums.violascore

import org.junit.Test
import org.junit.Assert.assertEquals
import org.palliums.violascore.utils.HexUtils
import org.palliums.violascore.wallet.Account
import org.palliums.libracore.wallet.KeyPair


class KeyPairTest {
    @Test
    fun test_generate_mnemonics_keypair() {
        val mnemonics = arrayListOf(
            "museum",
            "liquid",
            "spider",
            "explain",
            "vicious",
            "pave",
            "silent",
            "allow",
            "depth",
            "you",
            "adjust",
            "begin"
        )

        val keyPair = KeyPair.fromMnemonic(mnemonics)

        println("private key: ${HexUtils.toHex(keyPair.getPrivateKey())}")
        println("public key: ${HexUtils.toHex(keyPair.getPublicKey())}")
        println("address: ${Account(keyPair).getAddress().toHex()}")
        assertEquals(
            HexUtils.toHex(keyPair.getPrivateKey()),
            "ed7cc526bc39db7c754f1f90fbb5b7f7ce3499bee04e7525c3c599fcaa46aaea"
        )
        assertEquals(
            HexUtils.toHex(keyPair.getPublicKey()),
            "497681a1305fea13037794665286e0d185ea14a4656f89a00d87a9a33d336dd7"
        )
        assertEquals(
            Account(keyPair).getAddress().toHex(),
            "65e39e2e6b90ac215ec79e2b84690421d7286e6684b0e8e08a0b25dec640d849"
        )
    }

    @Test
    fun test_sign() {
        val keyPair =
            KeyPair(HexUtils.fromHex("ed7cc526bc39db7c754f1f90fbb5b7f7ce3499bee04e7525c3c599fcaa46aaea"))
        val message = byteArrayOf(0x1)
        val messageSign = keyPair.signRawTransaction(message)
        assertEquals(
            HexUtils.toHex(messageSign),
            "e62313d4966a73578bf89aef7b6c22ae92a113ded00ea9b0957e5e06719d00881af3d35dee1404ca1a74f6b3e47f23a9f016ebd6a717a77eea0f59fca6f86b0b"
        )
    }
}
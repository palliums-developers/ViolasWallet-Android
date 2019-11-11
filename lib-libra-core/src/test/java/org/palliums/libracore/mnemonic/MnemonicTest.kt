package org.palliums.libracore.mnemonic

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.palliums.libracore.utils.ByteUtility
import org.palliums.libracore.utils.HexUtils
import org.palliums.libracore.wallet.MNEMONIC_SALT_PREFIX
import org.spongycastle.crypto.PBEParametersGenerator
import org.spongycastle.crypto.digests.SHA3Digest
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.params.KeyParameter


class MnemonicTest {

    @Test
    fun test_generate_mnemonic() {
        val generate = Mnemonic(English.INSTANCE)
            .generate()
        print(generate)
    }

    @Test
    fun test_mnemonic_validation() {
        val mnemonics = arrayListOf(
            "regular",
            "grit",
            "build",
            "census",
            "idle",
            "insane",
            "tragic",
            "tiny",
            "miracle",
            "skirt",
            "midnight",
            "car"
        )

        val validation = Mnemonic(English.INSTANCE)
            .validation(mnemonics)
        assertEquals(validation, true)
    }

    @Test
    fun test_mnemonic_validation_false() {
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
            "add",
            "adjust",
            "begin"
        )

        val validation = Mnemonic(English.INSTANCE)
            .validation(mnemonics)
        assertEquals(validation, false)
    }

    @Test
    fun test_generate_private() {
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

        val passphrase = "LIBRA".toByteArray()
        val mnemonicSalt = MNEMONIC_SALT_PREFIX.toByteArray()

        val mnemonic = Mnemonic(English.INSTANCE)
            .toCharArray(mnemonics) ?: return

        val salt = ByteUtility.combine(mnemonicSalt, passphrase)

        val generator = PKCS5S2ParametersGenerator(SHA3Digest(256))
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(mnemonic), salt, 2048)
        val key = generator.generateDerivedMacParameters(256) as KeyParameter

        val hashAlgorithm = key.key

        assertEquals(
            HexUtils.toHex(hashAlgorithm),
            "ed7cc526bc39db7c754f1f90fbb5b7f7ce3499bee04e7525c3c599fcaa46aaea"
        )
    }

    @Test
    fun test_generated_mnemonic_words() {
        val mnemonic = Mnemonic(English.INSTANCE)
        for (i in 0..100000) {
            val list = mnemonic.generate()
            if (!mnemonic.validation(list)) {
                System.err.println(list)
                Assert.assertTrue(false)
            }
        }
    }
}
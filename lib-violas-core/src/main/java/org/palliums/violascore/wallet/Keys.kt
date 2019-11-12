package org.palliums.violascore.wallet

import com.google.common.primitives.Bytes
import org.palliums.violascore.crypto.ED25519
import org.palliums.violascore.mnemonic.English
import org.palliums.violascore.mnemonic.Mnemonic
import org.palliums.violascore.serialization.LCS
import org.spongycastle.crypto.digests.SHA3Digest
import org.spongycastle.crypto.generators.HKDFBytesGenerator
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.HKDFParameters
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.jcajce.provider.digest.SHA3
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.Strings
import java.security.Security
import java.text.Normalizer

/**
 * Created by elephant on 2019-09-20 14:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

class Seed {

    companion object {
        fun fromMnemonic(mnemonic: List<String>, salt: String = MNEMONIC_SALT_DEFAULT): Seed {
            require(mnemonic.isNotEmpty() && mnemonic.size % 6 == 0) {
                "Mnemonic must have a word count divisible with 6"
            }
            require(Mnemonic(English.INSTANCE).validation(mnemonic)) {
                "Mnemonic contains an unknown word"
            }
            require(salt.isNotEmpty()) { "Salt must not be empty" }

            val mnemonicBytes: ByteArray =
                Strings.toUTF8ByteArray(Mnemonic(English.INSTANCE).toCharArray(mnemonic))

            val saltBytes: ByteArray =
                Normalizer.normalize("$MNEMONIC_SALT_PREFIX$salt", Normalizer.Form.NFKD)
                    .toByteArray()

            val generator = PKCS5S2ParametersGenerator(SHA3Digest(256))
            generator.init(mnemonicBytes, saltBytes, 2048)

            val keyParameter: KeyParameter =
                generator.generateDerivedMacParameters(256) as KeyParameter
            return Seed(keyParameter.key)
        }
    }

    val data: ByteArray

    constructor(data: ByteArray) {
        require(data.size == 32) { "Seed data length must be 32 bits" }

        this.data = data
    }
}

class KeyFactory {

    companion object {
        init {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private val seed: Seed
    val masterPrk: ByteArray // master private key

    constructor(seed: Seed) {
        this.seed = seed

        val hMac = HMac(SHA3Digest(256))
        this.masterPrk = ByteArray(hMac.macSize)
        hMac.init(KeyParameter(MASTER_KEY_SALT.toByteArray()))
        hMac.update(seed.data, 0, seed.data.size)
        hMac.doFinal(this.masterPrk, 0)
    }

    fun generateKey(childDepth: Long): KeyPair {
        val info: ByteArray = Bytes.concat(DERIVED_KEY.toByteArray(), LCS.encodeLong(childDepth))

        val hkdfBytesGenerator = HKDFBytesGenerator(SHA3Digest(256))
        hkdfBytesGenerator.init(HKDFParameters.skipExtractParameters(this.masterPrk, info))

        val secretKey = ByteArray(32)
        hkdfBytesGenerator.generateBytes(secretKey, 0, 32)
        return KeyPair.fromSecretKey(secretKey)
    }
}

class KeyPair(
    private val secretKey: ByteArray
) {

    companion object {
        fun fromSecretKey(secretKey: ByteArray): KeyPair {
            return KeyPair(secretKey)
        }

        fun fromMnemonic(mnemonics: List<String>): KeyPair {
            return fromSecretKey(Seed.fromMnemonic(mnemonics).data)
        }
    }

    private val publicKey: ByteArray = ED25519.publickey(secretKey)

    fun getPrivateKey(): ByteArray {
        return secretKey
    }

    fun getPublicKey(): ByteArray {
        return publicKey
    }

    fun sign(message: ByteArray): ByteArray {
        val sha3256 = SHA3.Digest256()
        sha3256.update(SHA3.Digest256().digest(RAW_TRANSACTION_HASH_SALT.toByteArray()))
        sha3256.update(message)
        return ED25519.signature(sha3256.digest(), secretKey, publicKey)
    }
}
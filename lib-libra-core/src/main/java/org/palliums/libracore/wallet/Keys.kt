package org.palliums.libracore.wallet

import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.palliums.libracore.mnemonic.English
import org.palliums.libracore.mnemonic.Mnemonic
import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.utils.ByteUtility
import org.spongycastle.crypto.digests.SHA3Digest
import org.spongycastle.crypto.generators.HKDFBytesGenerator
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.HKDFParameters
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.jcajce.provider.digest.SHA3
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.Strings
import java.security.MessageDigest
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
        val info: ByteArray = ByteUtility.concat(DERIVED_KEY.toByteArray(), LCS.encodeLong(childDepth))

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
    private val mDsaNamedCurveSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    private val mEdDSAPrivateKey =
        EdDSAPrivateKey(EdDSAPrivateKeySpec(secretKey, mDsaNamedCurveSpec))
    private val mEdDSAPublicKey =
        EdDSAPublicKey(EdDSAPublicKeySpec(mEdDSAPrivateKey.a, mEdDSAPrivateKey.params))

    companion object {
        fun fromSecretKey(secretKey: ByteArray): KeyPair {
            return KeyPair(secretKey)
        }

        fun fromMnemonic(mnemonics: List<String>): KeyPair {
            return fromSecretKey(Seed.fromMnemonic(mnemonics).data)
        }
    }

    fun getPrivateKey(): ByteArray {
        return secretKey
    }

    fun getPublicKey(): ByteArray {
        return mEdDSAPublicKey.abyte
    }

    fun signRawTransaction(message: ByteArray): ByteArray {
        val sha3256 = SHA3.Digest256()
        sha3256.update(SHA3.Digest256().digest(RAW_TRANSACTION_HASH_SALT.toByteArray()))
        sha3256.update(message)

        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initSign(mEdDSAPrivateKey)
        edDSAEngine.update(sha3256.digest())
        return edDSAEngine.sign()
    }

    fun signMessage(message: ByteArray): ByteArray {
        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initSign(mEdDSAPrivateKey)
        edDSAEngine.update(message)
        return edDSAEngine.sign()
    }

    fun signSimple(data: ByteArray): ByteArray {
        val sha3256 = SHA3.Digest256()
        sha3256.update(data)

        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initSign(mEdDSAPrivateKey)
        edDSAEngine.update(sha3256.digest())
        return edDSAEngine.sign()
    }

    fun verifySimple(data: ByteArray, signedData: ByteArray): Boolean {
        val sha3256 = SHA3.Digest256()
        sha3256.update(data)

        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initVerify(mEdDSAPublicKey)
        edDSAEngine.update(sha3256.digest())
        return edDSAEngine.verify(signedData)
    }
}

private const val MAX_NUM_OF_KEYS = 32
const val BITMAP_NUM_OF_BYTES = 4

class MultiEd25519PublicKey(private val publicKeys: List<ByteArray>, private val threshold: Int) {

    init {
        if (threshold == 0 || publicKeys.size < threshold) {
            throw CryptoMaterialError.ValidationError()
        } else if (publicKeys.size > MAX_NUM_OF_KEYS) {
            throw CryptoMaterialError.WrongLengthError()
        }
    }

    fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        publicKeys.forEach {
            output.write(it)
        }
        output.writeU8(threshold)
        return output.toByteArray()
    }
}

class MultiEd25519PrivateKey(private val privateKeys: List<ByteArray>, private val threshold: Int) {
    init {
        if (threshold == 0 || privateKeys.size < threshold) {
            throw CryptoMaterialError.ValidationError()
        } else if (privateKeys.size > MAX_NUM_OF_KEYS) {
            throw CryptoMaterialError.WrongLengthError()
        }
    }

    fun signMessage(message: ByteArray): MultiEd25519Signature {
        val bitmap = ByteArray(BITMAP_NUM_OF_BYTES) { 0.toByte() }
        val signatures = ArrayList<ByteArray>()

        privateKeys.asIterable()
            .take(threshold)
            .mapIndexed { index, bytes ->
                bitmapSetBit(bitmap, index)
                signatures.add(KeyPair(bytes).signMessage(message))
            }

        return MultiEd25519Signature(signatures, bitmap)
    }
}

class MultiEd25519Signature(
    private val signatures: List<ByteArray>,
    private val bitmap: ByteArray
) {

    companion object {
//        fun new(signatures: List<ByteArray>, bitmap: Array<Byte>): MultiEd25519Signature {
//            val numOfSigSize = signatures.size
//            if (numOfSigSize == 0 || numOfSigSize > MAX_NUM_OF_KEYS) {
//                throw CryptoMaterialError.ValidationError()
//            }
//            val signatures = signatures.sortedBy {
//                it[1]
//            }
//            val bitmap = Array(BITMAP_NUM_OF_BYTES) { 0.toByte() }
//            for (i in 0 until bitmap.size) {
//                if (i < MAX_NUM_OF_KEYS) {
//                    if (bitmapGetBit(bitmap, i)) {
//                        throw CryptoMaterialError.BitVecError(
//                            "Duplicate signature index"
//                        )
//                    } else {
//                        bitmapSetBit(bitmap, i)
//                    }
//                } else {
//                    throw CryptoMaterialError.BitVecError("Signature index is out of range")
//                }
//            }
//            return MultiEd25519Signature(signatures, bitmap)
//        }
    }

    fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        signatures.forEach {
            output.write(it)
        }
        output.write(bitmap)
        return output.toByteArray()
    }
}

fun bitmapSetBit(bitmap: ByteArray, index: Int) {
    val bucketIndex = index / 8
    val innerIndex = index % 8
    bitmap[bucketIndex] = (128.ushr(innerIndex) or bitmap[bucketIndex].toInt()).toByte()
}

fun bitmapGetBit(bitmap: ByteArray, index: Int): Boolean {
    val bucketIndex = index / 8
    val innerIndex = index % 8
    return (128.ushr(innerIndex) and bitmap[bucketIndex].toInt()) != 0
}

fun bitmapLastGetBit(bitmap: ByteArray): Int {
    for (i in BITMAP_NUM_OF_BYTES - 1 downTo 0) {
        for (j in 0..7) {
            if (bitmap[i].toInt().ushr(j) and 0b1 == 1) {
                return i * 8 + (8 - j) - 1
            }
        }
    }
    return 0
}

fun bitmapCountOnes(bitmap: ByteArray): Int {
    var count = 0
    for (i in BITMAP_NUM_OF_BYTES - 1 downTo 0) {
        for (j in 0..7) {
            if (bitmap[i].toInt().ushr(j) and 0b1 == 1) {
                count++
            }
        }
    }
    return count
}
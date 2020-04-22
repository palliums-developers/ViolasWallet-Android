package org.palliums.libracore.crypto

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.wallet.CryptoMaterialError
import java.lang.RuntimeException

private const val MAX_NUM_OF_KEYS = 32

class MultiEd25519PublicKey(
    private val publicKeys: List<Ed25519PublicKey>,
    private val threshold: Int
) : KeyPair.PublicKey {

    init {
        if (threshold == 0 || publicKeys.size < threshold) {
            throw CryptoMaterialError.ValidationError()
        } else if (publicKeys.size > MAX_NUM_OF_KEYS) {
            throw CryptoMaterialError.WrongLengthError()
        }
    }

    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        publicKeys.forEach {
            output.write(it.toByteArray())
        }
        output.writeU8(threshold)
        return output.toByteArray()
    }
}

class MultiEd25519PrivateKey(
    private val privateKeys: List<Ed25519PrivateKeyIndex>,
    private val threshold: Int
) : KeyPair.PrivateKey {
    init {
        if (threshold == 0 || privateKeys.size < threshold) {
            throw CryptoMaterialError.ValidationError()
        } else if (privateKeys.size > MAX_NUM_OF_KEYS) {
            throw CryptoMaterialError.WrongLengthError()
        }
    }

    fun getPrivateKeys(): List<Ed25519PrivateKey> {
        return privateKeys
    }

    fun getThreshold(): Int {
        return threshold
    }

    fun signMessage(message: ByteArray): MultiEd25519Signature {
        val bitmap = Bitmap()
        val signatures = ArrayList<Signature>()

        privateKeys
            .sortedBy { it.getIndex() }
            .asIterable()
            .take(threshold)
            .mapIndexed { _, bytes ->
                bitmap.setBit(bytes.getIndex())
                signatures.add(Ed25519KeyPair(bytes.toByteArray()).signMessage(message))
            }

        return MultiEd25519Signature(signatures, bitmap)
    }

    override fun toByteArray(): ByteArray = byteArrayOf()
}

class MultiEd25519Signature(
    private val signatures: List<Signature>,
    private val bitmap: Bitmap
) : Signature {

    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        signatures.forEach {
            output.write(it.toByteArray())
        }
        output.write(bitmap.toByteArray())
        return output.toByteArray()
    }

    class Builder {
        private val mSignaturesMap = mutableMapOf<Int, Signature>()

        fun addSignature(index: Int, signature: Signature): Builder {
            mSignaturesMap[index] = signature
            return this
        }

        fun build(): MultiEd25519Signature {
            val signatures = mutableListOf<Signature>()
            val bitmap = Bitmap()

            val numOfSigSize = mSignaturesMap.size
            if (numOfSigSize == 0 || numOfSigSize > MAX_NUM_OF_KEYS) {
                throw CryptoMaterialError.ValidationError()
            }

            mSignaturesMap.keys.sorted().forEach { index ->
                if (index < MAX_NUM_OF_KEYS) {
                    mSignaturesMap[index]?.let {
                        if (bitmap.getBit(index)) {
                            throw CryptoMaterialError.BitVecError(
                                "Duplicate signature index"
                            )
                        } else {
                            bitmap.setBit(index)
                            signatures.add(it)
                        }
                    }
                } else {
                    throw CryptoMaterialError.BitVecError("Signature index is out of range")
                }
            }
            return MultiEd25519Signature(signatures, bitmap)
        }
    }
}

class MultiEd25519KeyPair(private val multiEd25519PrivateKey: MultiEd25519PrivateKey) : KeyPair {

    companion object {
        private val mDsaNamedCurveSpec =
            EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    }

    override fun getPrivateKey(): MultiEd25519PrivateKey {
        return multiEd25519PrivateKey
    }

    override fun getPublicKey(): MultiEd25519PublicKey {
        val privateKeys = multiEd25519PrivateKey.getPrivateKeys()
        val publicKeys = ArrayList<Ed25519PublicKey>(privateKeys.size)
        privateKeys.forEach {
            publicKeys.add(KeyPair.fromSecretKey(it.toByteArray()).getPublicKey())
        }
        return MultiEd25519PublicKey(publicKeys, multiEd25519PrivateKey.getThreshold())
    }

    override fun signMessage(message: ByteArray): MultiEd25519Signature {
        return multiEd25519PrivateKey.signMessage(message)
    }

    override fun verify(signed: Signature, message: ByteArray): Boolean {
        // todo
        return true
    }
}

class Bitmap(private val bitmap: ByteArray = ByteArray(BITMAP_NUM_OF_BYTES) { 0.toByte() }) {
    companion object {
        const val BITMAP_NUM_OF_BYTES = 4
    }

    init {
        if (bitmap.size > 4) {
            throw RuntimeException("Bitmap WrongLengthError")
        }
    }

    fun setBit(index: Int) {
        val bucketIndex = index / 8
        val innerIndex = index % 8
        bitmap[bucketIndex] = (128.ushr(innerIndex) or bitmap[bucketIndex].toInt()).toByte()
    }

    fun getBit(index: Int): Boolean {
        val bucketIndex = index / 8
        val innerIndex = index % 8
        return (128.ushr(innerIndex) and bitmap[bucketIndex].toInt()) != 0
    }

    fun lastGetBit(): Int {
        for (i in BITMAP_NUM_OF_BYTES - 1 downTo 0) {
            for (j in 0..7) {
                if (bitmap[i].toInt().ushr(j) and 0b1 == 1) {
                    return i * 8 + (8 - j) - 1
                }
            }
        }
        return 0
    }

    fun countOnes(): Int {
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

    fun toByteArray(): ByteArray {
        return bitmap.clone()
    }
}
package org.palliums.libracore.crypto

import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.wallet.CryptoMaterialError
import org.palliums.libracore.wallet.KeyPair
import java.lang.RuntimeException

private const val MAX_NUM_OF_KEYS = 32

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
        val bitmap = Bitmap()
        val signatures = ArrayList<ByteArray>()

        privateKeys.asIterable()
            .take(threshold)
            .mapIndexed { index, bytes ->
                bitmap.setBit(index)
                signatures.add(KeyPair(bytes).signMessage(message))
            }

        return MultiEd25519Signature(signatures, bitmap)
    }
}

class MultiEd25519PrivateKeyIndex(
    private val privateKeys: List<Pair<ByteArray, Int>>,
    private val threshold: Int
) {
    init {
        if (threshold == 0 || privateKeys.size < threshold) {
            throw CryptoMaterialError.ValidationError()
        } else if (privateKeys.size > MAX_NUM_OF_KEYS) {
            throw CryptoMaterialError.WrongLengthError()
        }
    }

    fun signMessage(message: ByteArray): MultiEd25519Signature {
        val bitmap = Bitmap()
        val signatures = ArrayList<ByteArray>()

        privateKeys
            .sortedBy { it.second }
            .asIterable()
            .take(threshold)
            .mapIndexed { _, bytes ->
                bitmap.setBit(bytes.second)
                signatures.add(KeyPair(bytes.first).signMessage(message))
            }

        return MultiEd25519Signature(signatures, bitmap)
    }
}

class MultiEd25519Signature(
    private val signatures: List<ByteArray>,
    private val bitmap: Bitmap
) {

    fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        signatures.forEach {
            output.write(it)
        }
        output.write(bitmap.toByteArray())
        return output.toByteArray()
    }

    class Builder {
        private val mSignaturesMap = mutableMapOf<Int, ByteArray>()

        fun addSignature(index: Int, signature: ByteArray): Builder {
            mSignaturesMap[index] = signature
            return this
        }

        fun build(): MultiEd25519Signature {
            val signatures = mutableListOf<ByteArray>()
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
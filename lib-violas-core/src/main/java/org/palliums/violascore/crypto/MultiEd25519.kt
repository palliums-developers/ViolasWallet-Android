package org.palliums.violascore.crypto

import org.palliums.violascore.serialization.LCSOutputStream
import org.palliums.violascore.wallet.CryptoMaterialError
import org.palliums.violascore.wallet.KeyPair
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

class MultiEd25519Signature(
    private val signatures: List<ByteArray>,
    private val bitmap: Bitmap
) {

//    companion object {
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
//    }

    fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        signatures.forEach {
            output.write(it)
        }
        output.write(bitmap.toByteArray())
        return output.toByteArray()
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
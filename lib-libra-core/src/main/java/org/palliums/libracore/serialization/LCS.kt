package org.palliums.libracore.serialization

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN

object LCS {
    fun encodeBool(b: Boolean): ByteArray {
        return if (b)
            byteArrayOf(0x01)
        else
            byteArrayOf(0x00)
    }

    fun encodeInt(value: Int): ByteArray {
//        val byteArray = ByteArray(4)
//        for (i in 0 until byteArray.size) {
//            byteArray[i] = value.shr(8 * i).and(0xFF).toByte()
//        }
//        return byteArray
        return ByteBuffer.allocate(4).order(LITTLE_ENDIAN).putInt(value).array()
    }

    fun encodeLong(value: Long): ByteArray {
        return ByteBuffer.allocate(8).order(LITTLE_ENDIAN).putLong(value).array()
    }

    fun encodeShort(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(LITTLE_ENDIAN).putShort(value).array()
    }

    fun encodeU8(value: Int): ByteArray{
        return ByteBuffer.allocate(1).order(LITTLE_ENDIAN).put(value.and(0xFF).toByte()).array()
    }

    fun encodeByte(value: Byte): ByteArray {
        return byteArrayOf(value)
    }

    fun encodeBytes(byteArrayOf: ByteArray): ByteArray {
        return encodeIntIndex(byteArrayOf.size).plus(byteArrayOf)
    }

    fun encodeShorts(shortArrayOf: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArrayOf.size * 2 + 4)
        byteArray.putAll(encodeIntIndex(shortArrayOf.size))
        var i = 4
        shortArrayOf.forEach {
            byteArray.putAll(encodeShort(it), i)
            i += 2
        }
        return byteArray
    }

    fun encodeString(str: String): ByteArray {
        val toByteArray = str.toByteArray()
        return encodeIntIndex(toByteArray.size).plus(toByteArray)
    }

    fun encodeByteArrayList(byteArrays: List<ByteArray>): ByteArray {
        var result = encodeIntIndex(byteArrays.size)
        byteArrays.forEach {
            result = result.plus(encodeBytes(it))
        }
        return result
    }

    fun encodeStrings(arrayListOf: Array<String>): ByteArray {
        var result = encodeIntIndex(arrayListOf.size)
        arrayListOf.forEach {
            result = result.plus(encodeString(it))
        }
        return result
    }

    fun encodeIntAsULEB128(value: Int): ByteArray {
        var handleValue = value
        val result = mutableListOf<Byte>()
        while (handleValue >= 0x80) {
            val byte = handleValue.and(0X7F).or(0x80).toByte()
            result.add(byte)
            handleValue = handleValue.shr(7)
        }
        result.add(handleValue.toByte())
        return result.toByteArray()
    }

    fun encodeIntIndex(value: Int): ByteArray {
        return encodeIntAsULEB128(value)
    }

    fun decodeBool(value: ByteArray): Boolean {
        return decodeBool(value[0])
    }

    fun decodeBool(value: Byte): Boolean {
        return 0x01.toByte() == value
    }

    fun decodeByte(value: ByteArray): Byte {
        return value[0]
    }

    fun decodeInt(value: ByteArray): Int {
        return ByteBuffer.wrap(value).order(LITTLE_ENDIAN).int
    }

    fun decodeShort(value: ByteArray): Short {
        return ByteBuffer.wrap(value).order(LITTLE_ENDIAN).short
    }

    fun decodeLong(value: ByteArray): Long {
        return ByteBuffer.wrap(value).order(LITTLE_ENDIAN).long
    }

    fun decodeU8(value: ByteArray): Int{
        return ByteBuffer.wrap(value).order(LITTLE_ENDIAN).int
    }

    fun decodeIntAsULEB128(value: InputStream): Int {
        var result = 0
        var cur: Int
        var index = 0

        do {
            cur = value.read() and 0xff
            result = result or (cur and 0x7f shl index * 7)
            index++
        } while (cur and 0x80 == 0x80 && index < 5)

        if (cur and 0x80 == 0x80) {
            return 0
        }
        return result
    }

    fun decodeIntIndex(value: InputStream): Int {
        return decodeIntAsULEB128(value)
    }
}

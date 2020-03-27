package org.palliums.libracore.serialization

import java.io.ByteArrayInputStream

class LCSInputStream(array: ByteArray) : ByteArrayInputStream(array) {

    fun readBool(): Boolean {
        return LCS.decodeBool(readByte())
    }

    fun readShort(): Short {
        val value = ByteArray(2)
        read(value)
        return LCS.decodeShort(value)
    }

    fun readInt(): Int {
        val value = ByteArray(4)
        read(value)
        return LCS.decodeInt(value)
    }

    fun readLong(): Long {
        val value = ByteArray(8)
        read(value)
        return LCS.decodeLong(value)
    }

    fun readByte(): Byte {
        val value = ByteArray(1)
        read(value)
        return value[0]
    }

    fun readBytes(): ByteArray {
        val readInt = readInt()
        val value = ByteArray(readInt)
        read(value)
        return value
    }

    fun readAddress(): ByteArray {
        val value = ByteArray(16)
        read(value)
        return value
    }

    fun readBytesList(): List<ByteArray> {
        val listSize = readInt()
        val listValue = ArrayList<ByteArray>(listSize)
        for (i in 0 until listSize) {
            val valueSize = readInt()
            val value = ByteArray(valueSize)
            read(value)
            listValue.add(value)
        }
        return listValue
    }

    fun readString(): String {
        val readInt = readInt()
        val value = ByteArray(readInt)
        read(value)
        return String(value)
    }
}
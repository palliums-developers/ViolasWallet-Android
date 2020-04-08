package org.palliums.libracore.serialization

import java.io.ByteArrayOutputStream

class LCSOutputStream : ByteArrayOutputStream() {
    fun writeShort(value: Short) {
        write(LCS.encodeShort(value))
    }

    fun writeU8(value:Int){
        write(LCS.encodeU8(value))
    }

    fun writeInt(value: Int) {
        write(LCS.encodeInt(value))
    }

    fun writeLong(value: Long) {
        write(LCS.encodeLong(value))
    }

    fun writeByte(value: Byte) {
        write(LCS.encodeByte(value))
    }

    fun writeBytes(value: ByteArray) {
        write(LCS.encodeBytes(value))
    }

    fun writeBytesList(value: List<ByteArray>) {
        write(LCS.encodeByteArrayList(value))
    }

    fun writeString(value: String) {
        write(LCS.encodeString(value))
    }

    fun writeIntAsLEB128(value: Int){
        write(LCS.encodeIntAsULEB128(value))
    }
}
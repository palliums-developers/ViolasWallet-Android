package org.palliums.violascore.serialization

fun ByteArray.toHex() = this.joinToString("") {
    String.format("%02x", it)
}

fun String.hexToBytes(): ByteArray {
    val s = this.replace(" ", "")
    val byteArray = ByteArray(s.length / 2)
    for (i in 0 until s.length / 2) {
        byteArray[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return byteArray
}

fun ByteArray.putAll(byteArray: ByteArray) {
    this.putAll(byteArray, 0)
}

fun ByteArray.putAll(byteArray: ByteArray, index: Int) {
    for (i in byteArray.indices) {
        this[i + index] = byteArray[i]
    }
}
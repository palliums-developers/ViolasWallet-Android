package org.palliums.libracore.serialization

private val HEX_CHARS = "0123456789abcdef".toCharArray()
private val toHEX = { b: ByteArray ->
    buildString {
        b.forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            append(HEX_CHARS[firstIndex])
            append(HEX_CHARS[secondIndex])
        }
    }
}

private val hexToBytes = { hex: String ->
    val len = hex.length
    val result = ByteArray(len / 2)
    (0 until len step 2).forEach { i ->
        result[i.shr(1)] =
            Character.digit(hex[i], 16).shl(4).or(Character.digit(hex[i + 1], 16)).toByte()
    }
    result
}

fun ByteArray.toHex() = toHEX(this)

fun String.hexToBytes() = hexToBytes(this)

fun ByteArray.putAll(byteArray: ByteArray) {
    this.putAll(byteArray, 0)
}

fun ByteArray.putAll(byteArray: ByteArray, index: Int) {
    for (i in byteArray.indices) {
        this[i + index] = byteArray[i]
    }
}
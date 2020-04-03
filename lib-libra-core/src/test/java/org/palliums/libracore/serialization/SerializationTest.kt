package org.palliums.libracore.serialization

import okhttp3.internal.toHexString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.palliums.libracore.utils.HexUtils
import java.io.ByteArrayInputStream
import java.io.InputStream

class SerializationTest {
    @Test
    fun test_bool() {
        val lcsTrue = LCS.encodeBool(true)
        assertEquals(lcsTrue.toHex(), "01".toLowerCase())

        val lcsFalse = LCS.encodeBool(false)
        assertEquals(lcsFalse.toHex(), "00".toLowerCase())


        assertEquals(LCS.decodeBool(lcsFalse), false)
        assertEquals(LCS.decodeBool(lcsTrue), true)
    }

    @Test
    fun test_ubyte() {
        val lcsInt = LCS.encodeByte(1)
        assertEquals(lcsInt.toHex(), "01")

        assertEquals(LCS.decodeByte(lcsInt), 1.toByte())
    }

    @Test
    fun test_byte() {
        val lcsInt = LCS.encodeByte(-1)
        assertEquals(lcsInt.toHex(), "FF".toLowerCase())

        assertEquals(LCS.decodeByte(lcsInt), (-1).toByte())
    }

    @Test
    fun test_ushort() {
        val lcsInt = LCS.encodeShort(4660)
        assertEquals(lcsInt.toHex(), "3412".toLowerCase())

        assertEquals(LCS.decodeShort(lcsInt), 4660.toShort())
    }

    @Test
    fun test_short() {
        val lcsInt = LCS.encodeShort(-4660)
        assertEquals(lcsInt.toHex(), "CCED".toLowerCase())

        assertEquals(LCS.decodeShort(lcsInt), (-4660).toShort())
    }

    @Test
    fun test_uint() {
        val lcsInt = LCS.encodeInt(305419896)
        assertEquals(lcsInt.toHex(), "78563412".toLowerCase())

        assertEquals(LCS.decodeInt(lcsInt), 305419896)
    }

    @Test
    fun test_int() {
        val lcsInt = LCS.encodeInt(-305419896)
        assertEquals(lcsInt.toHex(), "88A9CBED".toLowerCase())

        assertEquals(LCS.decodeInt(lcsInt), -305419896)
    }

    @Test
    fun test_intAsULEB128() {
        val lcsULong1 = LCS.encodeIntAsULEB128(1)
        assertEquals(lcsULong1.toHex(), "01".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong1)), 1)

        val lcsULong2 = LCS.encodeIntAsULEB128(128)
        assertEquals(lcsULong2.toHex(), "8001".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong2)), 128)

        val lcsULong3 = LCS.encodeIntAsULEB128(16384)
        assertEquals(lcsULong3.toHex(), "808001".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong3)), 16384)

        val lcsULong4 = LCS.encodeIntAsULEB128(2097152)
        assertEquals(lcsULong4.toHex(), "80808001".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong4)), 2097152)

        val lcsULong5 = LCS.encodeIntAsULEB128(268435456)
        assertEquals(lcsULong5.toHex(), "8080808001".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong5)), 268435456)

        val lcsULong6 = LCS.encodeIntAsULEB128(9487)
        assertEquals(lcsULong6.toHex(), "8f4a".toLowerCase())
        assertEquals(LCS.decodeIntAsULEB128(ByteArrayInputStream(lcsULong6)), 9487)
    }

    @Test
    fun test_ulong() {
        val lcsInt = LCS.encodeLong(1311768467750121216)
        assertEquals(lcsInt.toHex(), "00EFCDAB78563412".toLowerCase())

        assertEquals(LCS.decodeLong(lcsInt), 1311768467750121216L)

        val amount = 1
        val amountStr = amount.toString(16).padStart(16, '0').slice(0 until 16)
        val amountByteArray = HexUtils.fromHex(amountStr)
        amountByteArray.reverse()

        val lcsInt1 = LCS.encodeLong(1).toHex()
        assertEquals(lcsInt1, amountByteArray.toHex().toLowerCase())
    }

    @Test
    fun test_long() {
        val lcsInt = LCS.encodeLong(-1311768467750121216)
        assertEquals(lcsInt.toHex(), "0011325487A9CBED".toLowerCase())

        assertEquals(LCS.decodeLong(lcsInt), -1311768467750121216L)
    }

    @Test
    fun test_bytes() {
        val res1 = LCS.encodeBytes(byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55))
        assertEquals(res1.toHex(), "05 11 22 33 44 55".replace(" ", ""))

        val byteArray = ByteArray(6)
        byteArray.putAll(byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55))
        val res2 = LCS.encodeBytes(byteArray).toHex()
        assertEquals(res2, "06 11 22 33 44 55 00".replace(" ", ""))
    }

    @Test
    fun test_shorts() {
        val res1 = LCS.encodeShorts(shortArrayOf(0x11, 0x22)).toHex()
        assertEquals(res1, "02000000 1100 2200".replace(" ", ""))

        val byteArray = ShortArray(3)
        byteArray[0] = 0x11
        byteArray[1] = 0x22
        val res2 = LCS.encodeShorts(byteArray).toHex()
        assertEquals(res2, "03000000 1100 2200 0000".replace(" ", ""))
    }

    @Test
    fun test_string() {
        val res1 = LCS.encodeString("ሰማይ አይታረስ ንጉሥ አይከሰስ።").toHex()
        assertEquals(
            res1,
            "36 E188B0E1889BE18BAD20E18AA0E18BADE189B3E188A8E188B520E18A95E18C89E188A520E18AA0E18BADE18AA8E188B0E188B5E18DA2".toLowerCase().replace(" ", "")
        )
    }

    @Test
    fun test_list_bytearray() {
        val arrayListOf =
            arrayListOf(byteArrayOf(0x01, 0x02), byteArrayOf(0x11, 0x12, 0x13), byteArrayOf(0x21))
        val res1 = LCS.encodeByteArrayList(arrayListOf).toHex()
        assertEquals(
            res1,
            "03 02 0102 03 111213 01 21".replace(" ", "")
        )
    }

    @Test
    fun test_strings() {
        val arrayListOf = arrayOf("hello", "world")
        val res1 = LCS.encodeStrings(arrayListOf).toHex()
        assertEquals(
            res1,
            "02 05 68656c6c6f 05 776f726c64".replace(" ", "")
        )
    }
}
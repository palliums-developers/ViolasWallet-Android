package org.palliums.violascore.serialization

import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.palliums.violascore.transaction.*
import types.AccessPathOuterClass

class StructureTest {
    @Test
    fun test_Address() {
        val accountAddress =
            AccountAddress("ca820bf9305eb97d0d784f71b3955457fbf6911f5300ceaa5d7e8621529eae19".hexToBytes())
        assertEquals(
            accountAddress.toByteArray().toHex(),
            "20000000CA820BF9305EB97D0D784F71B3955457FBF6911F5300CEAA5D7E8621529EAE19".toLowerCase()
        )
    }

    @Test
    fun test_amount() {
        val encodeULong = TransactionArgument.newU64(9213671392124193148).toByteArray()
        assertEquals(encodeULong.toHex(), "000000007CC9BDA45089DD7F".toLowerCase())
    }

    @Test
    fun test_String() {
        val xxx = TransactionArgument.newString("Hello, World!").toByteArray()
        assertEquals(xxx.toHex(), "020000000D00000048656C6C6F2C20576F726C6421".toLowerCase())
    }

    @Test
    fun test_bytes() {
        val toByteArray = TransactionArgument.newByteArray("cafed00d".hexToBytes()).toByteArray()
        assertEquals(toByteArray.toHex(), "0300000004000000CAFED00D".toLowerCase())
    }

    @Test
    fun test_program() {
        val str1 = TransactionArgument.newString("CAFE D00D")
        val str2 = TransactionArgument.newString("cafe d00d")
        val toByteArray = TransactionPayload.Program(
            "move".toByteArray(),
            arrayListOf(str1, str2),
            arrayListOf("ca".hexToBytes(), "FED0".hexToBytes(), "0D".hexToBytes())
        ).toByteArray()



        assertEquals(
            toByteArray.toHex(),
            "040000006D6F766502000000020000000900000043414645204430304402000000090000006361666520643030640300000001000000CA02000000FED0010000000D".toLowerCase()
        )
    }

    @Test
    fun test_program_transfer() {
        val argumentAddress =
            TransactionArgument.newAddress("4fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c")
        val argumentAmount = TransactionArgument.newU64(1)
        val programByteArray =
            TransactionPayload.Program(
                "movemovemove".toByteArray(),
                arrayListOf(argumentAddress, argumentAmount),
                arrayListOf()
            ).toByteArray()
        assertEquals(
            programByteArray.toHex(),
            "0c0000006d6f76656d6f76656d6f76650200000001000000200000004fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c00000000010000000000000000000000".toLowerCase()
        )
    }

    @Test
    fun test_transactionPayload_program() {
        val str1 = TransactionArgument.newString("CAFE D00D")
        val str2 = TransactionArgument.newString("cafe d00d")
        val toByteArray = TransactionPayload(
            TransactionPayload.Program(
                "move".toByteArray(),
                arrayListOf(str1, str2),
                arrayListOf("ca".hexToBytes(), "FED0".hexToBytes(), "0D".hexToBytes())
            )
        ).toByteArray()
        assertEquals(
            toByteArray.toHex(),
            "00000000040000006D6F766502000000020000000900000043414645204430304402000000090000006361666520643030640300000001000000CA02000000FED0010000000D".toLowerCase()
        )
    }

    @Test
    fun test_transactionPayload_program_transfer() {
        val str1 =
            TransactionArgument.newAddress("4fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c")
        val str2 = TransactionArgument.newU64(1)
        val toByteArray = TransactionPayload(
            TransactionPayload.Program(
                "move".toByteArray(),
                arrayListOf(str1, str2),
                arrayListOf()
            )
        ).toByteArray()
        assertEquals(
            toByteArray.toHex(),
            "00000000040000006d6f76650200000001000000200000004fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c00000000010000000000000000000000".toLowerCase()
        )
    }

    @Test
    fun test_accessPath() {
        val accessPath = AccessPathOuterClass.AccessPath.newBuilder()
            .setAddress(ByteString.copyFrom("9a1ad09742d1ffc62e659e9a7797808b206f956f131d07509449c01ad8220ad4".hexToBytes()))
            .setPath(ByteString.copyFrom("01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()))
            .build()
        assertEquals(
            accessPath.toLcsBytes().toHex(),
            "200000009A1AD09742D1FFC62E659E9A7797808B206F956F131D07509449C01AD8220AD42100000001217DA6C6B3E19F1825CFB2676DAECCE3BF3DE03CF26647C78DF00B371B25CC97".toLowerCase()
        )
    }

    @Test
    fun test_WriteSet() {
        val writeOp1 = TransactionPayload.WriteOp(
            AccessPathOuterClass.AccessPath.newBuilder()
                .setAddress(ByteString.copyFrom("a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a".hexToBytes()))
                .setPath(ByteString.copyFrom("01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()))
                .build()
        )
        val writeOp2 = TransactionPayload.WriteOp(
            AccessPathOuterClass.AccessPath.newBuilder()
                .setAddress(ByteString.copyFrom("c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f".hexToBytes()))
                .setPath(ByteString.copyFrom("01217da6c6b3e19f18".hexToBytes()))
                .build()
            , "cafed00d".hexToBytes()
        )

        val writeSet = TransactionPayload.WriteSet(arrayListOf(writeOp1, writeOp2))

        assertEquals(
            writeSet.toByteArray().toHex(),
            "0200000020000000A71D76FAA2D2D5C3224EC3D41DEB293973564A791E55C6782BA76C2BF0495F9A2100000001217DA6C6B3E19F1825CFB2676DAECCE3BF3DE03CF26647C78DF00B371B25CC970000000020000000C4C63F80C74B11263E421EBF8486A4E398D0DBC09FA7D4F62CCDB309F3AEA81F0900000001217DA6C6B3E19F180100000004000000CAFED00D".toLowerCase()
        )
    }

    @Test
    fun test_transactionPayload_writeSet() {
        val writeOp1 = TransactionPayload.WriteOp(
            AccessPathOuterClass.AccessPath.newBuilder()
                .setAddress(ByteString.copyFrom("a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a".hexToBytes()))
                .setPath(ByteString.copyFrom("01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()))
                .build()
        )
        val writeOp2 = TransactionPayload.WriteOp(
            AccessPathOuterClass.AccessPath.newBuilder()
                .setAddress(ByteString.copyFrom("c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f".hexToBytes()))
                .setPath(ByteString.copyFrom("01217da6c6b3e19f18".hexToBytes()))
                .build()
            , "cafed00d".hexToBytes()
        )

        val writeSet =
            TransactionPayload(TransactionPayload.WriteSet(arrayListOf(writeOp1, writeOp2)))

        assertEquals(
            writeSet.toByteArray().toHex(),
            "010000000200000020000000A71D76FAA2D2D5C3224EC3D41DEB293973564A791E55C6782BA76C2BF0495F9A2100000001217DA6C6B3E19F1825CFB2676DAECCE3BF3DE03CF26647C78DF00B371B25CC970000000020000000C4C63F80C74B11263E421EBF8486A4E398D0DBC09FA7D4F62CCDB309F3AEA81F0900000001217DA6C6B3E19F180100000004000000CAFED00D".toLowerCase()
        )
    }

    @Test
    fun test_rawTransaction_program() {
        val str1 = TransactionArgument.newString("CAFE D00D")
        val str2 = TransactionArgument.newString("cafe d00d")
        val transactionPayload = TransactionPayload(
            TransactionPayload.Program(
                "move".toByteArray(),
                arrayListOf(str1, str2),
                arrayListOf("ca".hexToBytes(), "FED0".hexToBytes(), "0D".hexToBytes())
            )
        )

        val rawTransaction = RawTransaction(
            AccountAddress("3a24a61e05d129cace9e0efc8bc9e33831fec9a9be66f50fd352a2638a49b9ee".hexToBytes()),
            32,
            transactionPayload,
            10000,
            20000,
            86400
        )


        assertEquals(
            rawTransaction.toByteArray().toHex(),
            "200000003A24A61E05D129CACE9E0EFC8BC9E33831FEC9A9BE66F50FD352A2638A49B9EE200000000000000000000000040000006D6F766502000000020000000900000043414645204430304402000000090000006361666520643030640300000001000000CA02000000FED0010000000D1027000000000000204E0000000000008051010000000000".toLowerCase()
        )
    }
}
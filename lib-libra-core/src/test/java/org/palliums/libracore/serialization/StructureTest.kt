package org.palliums.libracore.serialization

import org.junit.Assert.assertEquals
import org.junit.Test
import org.palliums.libracore.transaction.*

class StructureTest {
    @Test
    fun test_Address() {
        val accountAddress =
            AccountAddress("ca820bf9305eb97d0d784f71b3955457fbf6911f5300ceaa5d7e8621529eae19".hexToBytes())
        assertEquals(
            accountAddress.toByteArray().toHex(),
            "ca820bf9305eb97d0d784f71b3955457fbf6911f5300ceaa5d7e8621529eae19".toLowerCase()
        )
    }

    @Test
    fun test_amount() {
        val encodeULong = TransactionArgument.newU64(9213671392124193148).toByteArray()
        assertEquals(encodeULong.toHex(), "000000007CC9BDA45089DD7F".toLowerCase())
    }

    @Test
    fun test_String() {
        val xxx = TransactionArgument.newByteArray("Hello, World!".toByteArray()).toByteArray()
        assertEquals(xxx.toHex(), "020000000d48656c6c6f2c20576f726c6421".toLowerCase())
    }

    @Test
    fun test_bytes() {
        val toByteArray = TransactionArgument.newByteArray("cafed00d".hexToBytes()).toByteArray()
        assertEquals(toByteArray.toHex(), "0200000004cafed00d".toLowerCase())
    }

//    @Test
//    fun test_program() {
//        val str1 = TransactionArgument.newByteArray("CAFE D00D".toByteArray())
//        val str2 = TransactionArgument.newByteArray("cafe d00d".toByteArray())
//        val toByteArray = TransactionPayload.Program(
//            "move".toByteArray(),
//            arrayListOf(str1, str2),
//            arrayListOf("ca".hexToBytes(), "FED0".hexToBytes(), "0D".hexToBytes())
//        ).toByteArray()
//
//
//
//        assertEquals(
//            toByteArray.toHex(),
//            "046d6f766502020000000943414645204430304402000000096361666520643030640301ca02fed0010d".toLowerCase()
//        )
//    }

//    @Test
//    fun test_program_transfer() {
//        val argumentAddress =
//            TransactionArgument.newAddress("4fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c")
//        val argumentAmount = TransactionArgument.newU64(1)
//        val programByteArray =
//            TransactionPayload.Program(
//                "movemovemove".toByteArray(),
//                arrayListOf(argumentAddress, argumentAmount),
//                arrayListOf()
//            ).toByteArray()
//        assertEquals(
//            programByteArray.toHex(),
//            "0c6d6f76656d6f76656d6f766502010000004fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c00000000010000000000000000".toLowerCase()
//        )
//    }

//    @Test
//    fun test_transactionPayload_program() {
//        val str1 = TransactionArgument.newByteArray("CAFE D00D".toByteArray())
//        val str2 = TransactionArgument.newByteArray("cafe d00d".toByteArray())
//        val toByteArray = TransactionPayload(
//            TransactionPayload.Program(
//                "move".toByteArray(),
//                arrayListOf(str1, str2),
//                arrayListOf("ca".hexToBytes(), "FED0".hexToBytes(), "0D".hexToBytes())
//            )
//        ).toByteArray()
//        assertEquals(
//            toByteArray.toHex(),
//            "00000000046d6f766502020000000943414645204430304402000000096361666520643030640301ca02fed0010d".toLowerCase()
//        )
//    }

//    @Test
//    fun test_transactionPayload_program_transfer() {
//        val str1 =
//            TransactionArgument.newAddress("4fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c")
//        val str2 = TransactionArgument.newU64(1)
//        val toByteArray = TransactionPayload(
//            TransactionPayload.Program(
//                "move".toByteArray(),
//                arrayListOf(str1, str2),
//                arrayListOf()
//            )
//        ).toByteArray()
//        assertEquals(
//            toByteArray.toHex(),
//            "00000000046d6f766502010000004fddcee027aa66e4e144d44dd218a345fb5af505284cb03368b7739e92dd6b3c00000000010000000000000000".toLowerCase()
//        )
//    }

    @Test
    fun test_accessPath() {
        val accessPath = AccessPath(
            AccountAddress("9a1ad09742d1ffc62e659e9a7797808b206f956f131d07509449c01ad8220ad4".hexToBytes()),
            "01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()
        )
        assertEquals(
            accessPath.toByteArray().toHex(),
            "209a1ad09742d1ffc62e659e9a7797808b206f956f131d07509449c01ad8220ad42101217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".toLowerCase()
        )
    }

    @Test
    fun test_WriteSet() {
        val writeOp1 = TransactionPayload.WriteOp(
            AccessPath(
                AccountAddress("a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a".hexToBytes()),
                "01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()
            )
        )
        val writeOp2 = TransactionPayload.WriteOp(
            AccessPath(
                AccountAddress("c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f".hexToBytes()),
                "01217da6c6b3e19f18".hexToBytes()
            )
            , "cafed00d".hexToBytes()
        )

        val writeSet = TransactionPayload.WriteSet(arrayListOf(writeOp1, writeOp2))

        assertEquals(
            writeSet.toByteArray().toHex(),
            "0220a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a2101217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc970000000020c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f0901217da6c6b3e19f180100000004cafed00d".toLowerCase()
        )
    }

    @Test
    fun test_transactionPayload_writeSet() {
        val writeOp1 = TransactionPayload.WriteOp(
            AccessPath(
                AccountAddress("a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a".hexToBytes()),
                "01217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc97".hexToBytes()
            )
        )
        val writeOp2 = TransactionPayload.WriteOp(
            AccessPath(
                AccountAddress("c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f".hexToBytes()),
                "01217da6c6b3e19f18".hexToBytes()
            )
            , "cafed00d".hexToBytes()
        )

        val writeSet =
            TransactionPayload(TransactionPayload.WriteSet(arrayListOf(writeOp1, writeOp2)))

        assertEquals(
            writeSet.toByteArray().toHex(),
            "010000000220a71d76faa2d2d5c3224ec3d41deb293973564a791e55c6782ba76c2bf0495f9a2101217da6c6b3e19f1825cfb2676daecce3bf3de03cf26647c78df00b371b25cc970000000020c4c63f80c74b11263e421ebf8486a4e398d0dbc09fa7d4f62ccdb309f3aea81f0901217da6c6b3e19f180100000004cafed00d".toLowerCase()
        )
    }
}
package org.palliums.libracore.transaction

import com.google.protobuf.ByteString
import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.utils.HexUtils
import types.Transaction
import types.AccessPathOuterClass.AccessPath


data class RawTransaction(
    val sender: AccountAddress,
    val sequence_number: Long,
    val payload: TransactionPayload?,
    val max_gas_amount: Long,
    val gas_unit_price: Long,
    val expiration_time: Long
) {
    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.write(sender.toByteArray())
        stream.writeLong(sequence_number)
        payload?.let {
            stream.write(payload.toByteArray())
        }
        stream.writeLong(max_gas_amount)
        stream.writeLong(gas_unit_price)
        stream.writeLong(expiration_time)
        return stream.toByteArray()
    }

    companion object {
        fun decode(input: LCSInputStream): RawTransaction {
            return RawTransaction(
                AccountAddress.decode(input),
                input.readLong(),
                TransactionPayload.decode(input),
                input.readLong(),
                input.readLong(),
                input.readLong()
            )
        }
    }
}

data class SignedTransaction(
    val rawTxn: RawTransaction,
    val publicKey: ByteArray,
    val signature: ByteArray
) {
    val transactionLength: Long

    init {
        transactionLength = rawTxn.toByteArray().size.toLong()
    }

    fun toByteArray(): ByteArray {
        println("public key size:${publicKey.size} hex:${LCS.encodeInt(publicKey.size).toHex()}")
        println("signature size:${signature.size} hex:${LCS.encodeInt(signature.size).toHex()}")
        val stream = LCSOutputStream()
        stream.write(rawTxn.toByteArray())
        stream.writeBytes(publicKey)
        stream.writeBytes(signature)
        return stream.toByteArray()
    }

    companion object {
        fun decode(array: ByteArray): SignedTransaction {
            LCSInputStream(array).use {
                return SignedTransaction(
                    RawTransaction.decode(it),
                    it.readBytes(),
                    it.readBytes()
                )
            }
        }
    }
}

data class TransactionPayload(val payload: Payload) {
    abstract class Payload(val type: Int) {
        abstract fun toByteArray(): ByteArray
    }

    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeInt(payload.type)
        stream.write(payload.toByteArray())
        return stream.toByteArray()
    }

    companion object {
        fun decode(input: LCSInputStream): TransactionPayload {
            val readInt = input.readInt()
            return TransactionPayload(
                when (readInt) {
                    0 -> {
                        Program.decode(input)
                    }
                    1 -> {
                        WriteSet.decode(input)
                    }
                    2 -> {
                        Script.decode(input)
                    }
                    3 -> {
                        Module.decode(input)
                    }
                    else -> {
                        Module(byteArrayOf(0))
                    }
                }
            )
        }
    }

    data class Program(
        val code: ByteArray,
        val args: List<TransactionArgument>,
        val modules: List<ByteArray>
    ) : Payload(0) {
        override fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.writeBytes(code)
            stream.writeInt(args.size)
            args.forEach {
                stream.write(it.toByteArray())
            }
            stream.writeBytesList(modules)
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): Program {
                val code = input.readBytes()
                val size = input.readInt()
                val args = ArrayList<TransactionArgument>(size)
                for (i in 0 until size) {
                    args.add(TransactionArgument.decode(input))
                }
                return Program(
                    code,
                    args,
                    input.readBytesList()
                )
            }
        }
    }

    data class Script(
        val code: ByteArray,
        val args: List<TransactionArgument>
    ) : TransactionPayload.Payload(2) {
        override fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.writeBytes(code)
            stream.writeInt(args.size)
            args.forEach {
                stream.write(it.toByteArray())
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): Script {
                val code = input.readBytes()
                val size = input.readInt()
                val args = ArrayList<TransactionArgument>(size)
                for (i in 0 until size) {
                    args.add(TransactionArgument.decode(input))
                }
                return Script(
                    code,
                    args
                )
            }
        }
    }

    data class Module(
        val code: ByteArray
    ) : TransactionPayload.Payload(3) {
        override fun toByteArray(): ByteArray {
            return LCS.encodeBytes(code)
        }

        companion object {
            fun decode(input: LCSInputStream): Module {
                return Module(
                    input.readBytes()
                )
            }
        }
    }

    data class WriteSet(
        val writeSet: List<WriteOp>
    ) : Payload(1) {
        override fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.writeInt(writeSet.size)
            writeSet.forEach {
                stream.write(it.toByteArray())
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): WriteSet {
                val size = input.readInt()
                val args = ArrayList<WriteOp>(size)
                for (i in 0 until size) {
                    args.add(WriteOp.decode(input))
                }
                return WriteSet(
                    args
                )
            }
        }
    }

    data class WriteOp(
        val accessPath: AccessPath,
        val value: ByteArray? = null
    ) {
        val type: WriteOpType

        init {
            if (value == null) {
                type = WriteOpType.Deletion
            } else {
                type = WriteOpType.Value
            }
        }

        fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.write(accessPath.toLcsBytes())
            stream.writeInt(type.ordinal)
            if (type == WriteOpType.Value && value != null) {
                stream.writeBytes(value)
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): WriteOp {

                val accessPath = AccessPath.newBuilder()
                    .setAddress(ByteString.copyFrom(input.readBytes()))
                    .setPath(ByteString.copyFrom(input.readBytes()))
                    .build()

                val type = input.readInt()
                if (type == WriteOpType.Value.ordinal) {
                    return WriteOp(
                        accessPath,
                        input.readBytes()
                    )
                } else {
                    return WriteOp(
                        accessPath
                    )
                }
            }
        }
    }

    enum class WriteOpType(value: Int) {
        Deletion(0),
        Value(1),
    }
}

data class TransactionArgument(
    val argType: Transaction.TransactionArgument.ArgType,
    val data: ByteArray
) {
    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeInt(argType.number)
        stream.write(data)
        return stream.toByteArray()
    }

    companion object {
        @JvmStatic
        fun newU64(value: Long): TransactionArgument {
            return TransactionArgument(
                Transaction.TransactionArgument.ArgType.U64,
                LCS.encodeLong(value)
            )
        }

        @JvmStatic
        fun newAddress(value: String): TransactionArgument {
            return newAddress(HexUtils.fromHex(value))
        }

        @JvmStatic
        fun newAddress(value: ByteArray): TransactionArgument {
            return TransactionArgument(
                Transaction.TransactionArgument.ArgType.ADDRESS,
                LCS.encodeBytes(value)
            )
        }

        @JvmStatic
        fun newString(value: String): TransactionArgument {
            return TransactionArgument(
                Transaction.TransactionArgument.ArgType.STRING,
                LCS.encodeString(value)
            )
        }

        @JvmStatic
        fun newByteArray(value: ByteArray): TransactionArgument {
            return TransactionArgument(
                Transaction.TransactionArgument.ArgType.BYTEARRAY,
                LCS.encodeBytes(value)
            )
        }

        fun decode(input: LCSInputStream): TransactionArgument {
            val readInt = input.readInt()
            return when (readInt) {
                Transaction.TransactionArgument.ArgType.U64.number -> {
                    newU64(input.readLong())
                }
                Transaction.TransactionArgument.ArgType.ADDRESS.number -> {
                    newAddress(input.readBytes())
                }
                Transaction.TransactionArgument.ArgType.STRING.number -> {
                    newString(input.readString())
                }
                Transaction.TransactionArgument.ArgType.BYTEARRAY.number -> {
                    newByteArray(input.readBytes())
                }
                else -> newByteArray(byteArrayOf(0))
            }
        }
    }

}

data class AccountAddress(val byte: ByteArray) {
    fun toByteArray(): ByteArray {
        return LCS.encodeBytes(byte)
    }

    companion object {
        fun decode(input: LCSInputStream): AccountAddress {
            return AccountAddress(
                input.readBytes()
            )
        }
    }
}

fun AccessPath.toLcsBytes(): ByteArray {
    val stream = LCSOutputStream()
    stream.writeBytes(this.address.toByteArray())
    stream.writeBytes(this.path.toByteArray())
    return stream.toByteArray()
}

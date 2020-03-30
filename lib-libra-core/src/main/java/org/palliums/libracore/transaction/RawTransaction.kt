package org.palliums.libracore.transaction

import com.google.protobuf.ByteString
import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.transaction.storage.TypeTagEnum
import org.palliums.libracore.utils.HexUtils
import org.palliums.libracore.wallet.AuthenticationKey
import org.palliums.libracore.wallet.TransactionAuthenticator
import types.AccessPathOuterClass.AccessPath


data class RawTransaction(
    val sender: AccountAddress,
    val sequence_number: Long,
    val payload: TransactionPayload?,
    val max_gas_amount: Long,
    val gas_unit_price: Long,
    val gas_specifier: TypeTag,
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
        stream.write(gas_specifier.toByteArray())
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
                TypeTag.decode(input),
                input.readLong()
            )
        }
    }
}

data class SignedTransaction(
    val rawTxn: RawTransaction,
    val authenticator: TransactionAuthenticator
) {
    val transactionLength: Long

    init {
        transactionLength = rawTxn.toByteArray().size.toLong()
    }

    fun toByteArray(): ByteArray {
//        println("public key size:${publicKey.size} hex:${LCS.encodeInt(publicKey.size).toHex()}")
//        println("signature size:${signature.size} hex:${LCS.encodeInt(signature.size).toHex()}")
        val stream = LCSOutputStream()
        stream.write(rawTxn.toByteArray())
        stream.write(authenticator.toByteArray())
        return stream.toByteArray()
    }

    companion object {
        fun decode(array: ByteArray): SignedTransaction {
            LCSInputStream(array).use {
                return SignedTransaction(
                    RawTransaction.decode(it),
                    TransactionAuthenticator.decode(it)
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
    val argType: ArgType,
    val data: ByteArray
) {
    enum class ArgType(val number: Int) {
        U64(0),
        ADDRESS(1),
        BYTEARRAY(2),
        BOOL(3),
    }

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
                ArgType.U64,
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
                ArgType.ADDRESS,
                value
            )
        }

        @JvmStatic
        fun newBool(value: Boolean): TransactionArgument {
            return TransactionArgument(
                ArgType.BOOL,
                LCS.encodeBool(value)
            )
        }

        @JvmStatic
        fun newByteArray(value: ByteArray): TransactionArgument {
            return TransactionArgument(
                ArgType.BYTEARRAY,
                LCS.encodeBytes(value)
            )
        }

        fun decode(input: LCSInputStream): TransactionArgument {
            val readInt = input.readInt()
            return when (readInt) {
                ArgType.U64.number -> {
                    newU64(input.readLong())
                }
                ArgType.ADDRESS.number -> {
                    val value = ByteArray(32)
                    input.read(value)
                    newAddress(value)
                }
                ArgType.BOOL.number -> {
                    newBool(input.readBool())
                }
                ArgType.BYTEARRAY.number -> {
                    newByteArray(input.readBytes())
                }
                else -> newByteArray(byteArrayOf(0))
            }
        }
    }

}

data class AccountAddress(val byte: ByteArray) {
    fun toByteArray(): ByteArray {
        return byte
    }

    companion object {
        val DEFAULT = AccountAddress(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))

        fun decode(input: LCSInputStream): AccountAddress {
            val value = ByteArray(32)
            input.read(value)
            return AccountAddress(
                value
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

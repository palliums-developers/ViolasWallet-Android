package org.palliums.libracore.transaction

import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.storage.TypeTag
import org.palliums.libracore.utils.HexUtils
import java.math.BigInteger

data class TransactionPayload(val payload: Payload) {
    abstract class Payload(val type: Int) {
        abstract fun toByteArray(): ByteArray
    }

    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeIntAsLEB128(payload.type)
        stream.write(payload.toByteArray())
        return stream.toByteArray()
    }

    companion object {
        fun decode(input: LCSInputStream): TransactionPayload {
            val readInt = input.readIntAsLEB128()
            return TransactionPayload(
                when (readInt) {
                    0 -> {
                        WriteSet.decode(input)
                    }
                    1 -> {
                        Script.decode(input)
                    }
                    2 -> {
                        Module.decode(input)
                    }
                    else -> {
                        Module(byteArrayOf(0))
                    }
                }
            )
        }
    }

    data class Script(
        val code: ByteArray,
        val tyArgs: List<TypeTag>,
        val args: List<TransactionArgument>
    ) : TransactionPayload.Payload(1) {
        override fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.writeBytes(code)
            stream.writeIntAsLEB128(tyArgs.size)
            tyArgs.forEach {
                stream.write(it.toByteArray())
            }
            stream.writeIntAsLEB128(args.size)
            args.forEach {
                stream.write(it.toByteArray())
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): Script {
                val code = input.readBytes()
                val tyArgsSize = input.readIntAsLEB128()
                val tyArgs = ArrayList<TypeTag>(tyArgsSize)
                for (i in 0 until tyArgsSize) {
                    tyArgs.add(TypeTag.decode(input))
                }

                val size = input.readIntAsLEB128()
                val args = ArrayList<TransactionArgument>(size)
                for (i in 0 until size) {
                    args.add(TransactionArgument.decode(input))
                }
                return Script(
                    code,
                    tyArgs,
                    args
                )
            }
        }
    }

    data class Module(
        val code: ByteArray
    ) : TransactionPayload.Payload(2) {
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
    ) : Payload(0) {
        override fun toByteArray(): ByteArray {
            val stream = LCSOutputStream()
            stream.writeIntAsLEB128(writeSet.size)
            writeSet.forEach {
                stream.write(it.toByteArray())
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): WriteSet {
                val size = input.readIntAsLEB128()
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
            stream.write(accessPath.toByteArray())
            stream.writeInt(type.ordinal)
            if (type == WriteOpType.Value && value != null) {
                stream.writeBytes(value)
            }
            return stream.toByteArray()
        }

        companion object {
            fun decode(input: LCSInputStream): WriteOp {

                val accessPath = AccessPath.decode(input)

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
        U8(0),
        U64(1),
        U128(2),
        ADDRESS(3),
        BYTEARRAY(4),
        BOOL(5),
    }

    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeIntAsLEB128(argType.number)
        stream.write(data)
        return stream.toByteArray()
    }

    fun decodeToValue(): Any {
        return when (argType) {
            ArgType.U8 -> LCS.decodeByte(data)
            ArgType.U64 -> LCS.decodeLong(data)
            ArgType.U128 -> LCS.decodeLong(data)
            ArgType.ADDRESS -> {
                data.toHex()
            }
            ArgType.BYTEARRAY -> {
                val lcsInputStream = LCSInputStream(data)
                lcsInputStream.readBytes()
            }
            ArgType.BOOL -> LCS.decodeBool(data)
        }
    }

    companion object {
        @JvmStatic
        fun newU8(value: Int): TransactionArgument {
            return TransactionArgument(
                ArgType.U8,
                LCS.encodeU8(value)
            )
        }

        @JvmStatic
        fun newU64(value: Long): TransactionArgument {
            return TransactionArgument(
                ArgType.U64,
                LCS.encodeLong(value)
            )
        }

        @JvmStatic
        fun newU128(value: BigInteger): TransactionArgument {
            return TransactionArgument(
                ArgType.U128,
                LCS.encodeLong(value.toLong())
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
            val readInt = input.readIntAsLEB128()
            return when (readInt) {
                ArgType.U8.number -> {
                    newU8(input.readU8())
                }
                ArgType.U64.number -> {
                    newU64(input.readLong())
                }
                // todo 读取 128
                ArgType.U128.number -> {
                    newU128(input.readLong().toBigInteger())
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
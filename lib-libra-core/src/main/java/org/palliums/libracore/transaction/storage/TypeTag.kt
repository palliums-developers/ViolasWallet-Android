package org.palliums.libracore.transaction.storage

import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.RawTransaction
import org.palliums.libracore.transaction.SignedTransaction
import org.palliums.libracore.transaction.TransactionArgument

enum class TypeTagEnum(val value: Int) {
    Bool(0),
    U8(1),
    U64(2),
    U128(3),
    Address(4),
    ListTypeTag(5),//Vector(Box<TypeTag>),
    StructTag(6);

    companion object {
        fun convert(value: Int): TypeTagEnum {
            return when (value) {
                0 -> Bool
                1 -> U8
                2 -> U64
                3 -> U128
                4 -> Address
                5 -> ListTypeTag
                6 -> StructTag
                else -> U64
            }
        }
    }
}

class TypeTag(
    val argType: TypeTagEnum,
    val data: ByteArray
) {
    companion object {
        fun newBool(boolean: Boolean): TypeTag {
            return TypeTag(TypeTagEnum.Bool, LCS.encodeBool(boolean))
        }

        fun newU8(value: Short): TypeTag {
            return TypeTag(TypeTagEnum.U8, LCS.encodeShort(value))
        }

        fun newU64(value: Long): TypeTag {
            return TypeTag(TypeTagEnum.U64, LCS.encodeLong(value))
        }

//       todo
//        fun newU128(value: Boolean): TypeTag {
//            return TypeTag(TypeTagEnum.U128,LCS.encodeBool(boolean))
//        }

        fun newAddress(value: AccountAddress): TypeTag {
            return TypeTag(TypeTagEnum.Bool, value.toByteArray())
        }

        fun newListTypeTag(value: List<TypeTag>): TypeTag {
            val output = LCSOutputStream()
            output.write(value.size)
            value.forEach {
                output.writeBytes(it.toByteArray())
            }
            return TypeTag(TypeTagEnum.ListTypeTag, output.toByteArray())
        }

        fun newStructTag(value: StructTag): TypeTag {
            return TypeTag(TypeTagEnum.StructTag, value.toByteArray())
        }

        fun decode(input: LCSInputStream): TypeTag {
            val typeTagType = input.readInt()
            when (typeTagType) {
                TypeTagEnum.Bool.value -> input.readBool()
                TypeTagEnum.U8.value -> input.readShort()
                TypeTagEnum.U64.value -> input.readLong()
                TypeTagEnum.U128.value -> {
                    // todo
                    input.readLong()
                    input.readLong()
                }
                TypeTagEnum.Address.value -> input.readAddress()
                TypeTagEnum.ListTypeTag.value -> {
                    val size = input.readInt()
                    val list = ArrayList<TypeTag>(size)
                    for (i in 0 until size) {
                        list.add(TypeTag.decode(input))
                    }
                }
                TypeTagEnum.StructTag.value -> {
                    val structTag = StructTag.decode(input)
                }
            }
            // todo
            return TypeTag(TypeTagEnum.convert(typeTagType), byteArrayOf())
        }
    }

    fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeInt(argType.value)
        output.write(data)
        return output.toByteArray()
    }
}

data class StructTag(
    private val address: AccountAddress,
    private val module: String,
    private val name: String,
    // TODO: rename to "type_args"
    private val type_params: ArrayList<TypeTagEnum>
) {
    companion object {
        fun decode(input: LCSInputStream): StructTag {
            val accountAddress = AccountAddress.decode(input)
            val module = input.readString()
            val name = input.readString()
            val size = input.readInt()
            val args = ArrayList<TypeTagEnum>(size)
            for (i in 0 until size) {
                val value = input.readInt()
                args.add(TypeTagEnum.convert(value))
            }
            return StructTag(
                accountAddress,
                module,
                name,
                args
            )
        }
    }

    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.write(address.toByteArray())
        stream.writeString(module)
        stream.writeString(name)
        stream.writeInt(type_params.size)
        type_params.forEach {
            stream.writeInt(it.value)
        }
        return stream.toByteArray()
    }
}
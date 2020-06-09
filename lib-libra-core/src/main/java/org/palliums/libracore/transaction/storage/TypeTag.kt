package org.palliums.libracore.transaction.storage

import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.transaction.AccountAddress

enum class TypeTagEnum(val value: Int) {
    Bool(0),
    U8(1),
    U64(2),
    U128(3),
    Address(4),
    Signer(5),
    ListTypeTag(6),//Vector(Box<TypeTag>),
    StructTag(7);

    companion object {
        fun convert(value: Int): TypeTagEnum {
            return when (value) {
                0 -> Bool
                1 -> U8
                2 -> U64
                3 -> U128
                4 -> Address
                5 -> Signer
                6 -> ListTypeTag
                7 -> StructTag
                else -> U64
            }
        }
    }
}

interface TypeTag {
    companion object {
        fun decode(input: LCSInputStream): TypeTag {
            return when (input.readInt()) {
                TypeTagEnum.Bool.value -> TypeTagBool(input.readBool())
                TypeTagEnum.U8.value -> TypeTagU8(input.readShort())
                TypeTagEnum.U64.value -> TypeTagU64(input.readLong())
                TypeTagEnum.U128.value -> {
                    // todo
                    input.readLong()
                    TypeTagU128(input.readLong())
                }
                TypeTagEnum.Address.value -> TypeTagAddress(AccountAddress(input.readAddress()))
                TypeTagEnum.Signer.value -> {
                    val list = ByteArray(32)
                    input.read(list)
                    TypeTagSigner(list)
                }
                TypeTagEnum.ListTypeTag.value -> {
                    val size = input.readIntAsLEB128()
                    val list = ArrayList<TypeTag>(size)
                    for (i in 0 until size) {
                        list.add(TypeTag.decode(input))
                    }
                    TypeTagListTypeTag(list)
                }
                TypeTagEnum.StructTag.value -> {
                    TypeTagStructTag(StructTag.decode(input))
                }
                else -> {
                    TypeTagBool(false)
                }
            }
        }

        fun newBool(value: Boolean): TypeTag {
            return TypeTagBool(value)
        }

        fun newU8(value: Short): TypeTag {
            return TypeTagU8(value)
        }

        fun newU64(value: Long): TypeTag {
            return TypeTagU64(value)
        }

        fun newU128(value: Long): TypeTag {
            return TypeTagU128(value)
        }

        fun newAddress(value: AccountAddress): TypeTag {
            return TypeTagAddress(value)
        }

        fun newSigner(value: ByteArray): TypeTag {
            return TypeTagSigner(value)
        }

        fun newListTypeTag(value: List<TypeTag>): TypeTag {
            return TypeTagListTypeTag(value)
        }

        fun newStructTag(value: StructTag): TypeTag {
            return TypeTagStructTag(value)
        }
    }

    fun toByteArray(): ByteArray
}

class TypeTagBool(val value: Boolean) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeInt(TypeTagEnum.Bool.value)
        output.write(LCS.encodeBool(value))
        return output.toByteArray()
    }
}

class TypeTagU8(val value: Short) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeInt(TypeTagEnum.U8.value)
        output.write(LCS.encodeShort(value))
        return output.toByteArray()
    }
}

class TypeTagU64(val value: Long) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeInt(TypeTagEnum.U64.value)
        output.write(LCS.encodeLong(value))
        return output.toByteArray()
    }
}

// todo
class TypeTagU128(val value: Long) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeIntAsLEB128(TypeTagEnum.U128.value)
//        output.write(LCS.encodeShort(value))
        return output.toByteArray()
    }
}

class TypeTagAddress(val value: AccountAddress) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeIntAsLEB128(TypeTagEnum.Address.value)
        output.write(value.toByteArray())
        return output.toByteArray()
    }
}

class TypeTagListTypeTag(val value: List<TypeTag>) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeIntAsLEB128(TypeTagEnum.ListTypeTag.value)
        output.writeIntAsLEB128(value.size)
        value.forEach {
            output.writeBytes(it.toByteArray())
        }
        return output.toByteArray()
    }
}

class TypeTagStructTag(val value: StructTag) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeIntAsLEB128(TypeTagEnum.StructTag.value)
        output.write(value.toByteArray())
        return output.toByteArray()
    }
}

class TypeTagSigner(val value: ByteArray) : TypeTag {
    override fun toByteArray(): ByteArray {
        val output = LCSOutputStream()
        output.writeIntAsLEB128(TypeTagEnum.StructTag.value)
        output.write(value)
        return output.toByteArray()
    }
}

data class StructTag(
    val address: AccountAddress,
    val module: String,
    val name: String,
    // TODO: rename to "type_args"
    val type_params: ArrayList<TypeTagEnum>
) {
    companion object {
        fun decode(input: LCSInputStream): StructTag {
            val accountAddress = AccountAddress.decode(input)
            val module = input.readString()
            val name = input.readString()
            val size = input.readIntAsLEB128()
            val args = ArrayList<TypeTagEnum>(size)
            for (i in 0 until size) {
                val value = input.readIntAsLEB128()
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
        stream.writeIntAsLEB128(type_params.size)
        type_params.forEach {
            stream.writeIntAsLEB128(it.value)
        }
        return stream.toByteArray()
    }
}
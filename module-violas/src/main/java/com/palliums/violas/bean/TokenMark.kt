package com.palliums.violas.bean

import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag

data class TokenMark(
    val module: String,
    val address: String,
    val name: String
) {
    companion object {
        fun convert(tokenA: TokenMark): TypeTagStructTag {
            return TypeTagStructTag(
                StructTag(
                    AccountAddress(tokenA.address.hexToBytes()),
                    tokenA.module,
                    tokenA.name,
                    arrayListOf()
                )
            )
        }
    }

    fun toTypeTag() = convert(this)

    override fun hashCode(): Int {
        var result = module.hashCode()
        result = result * 31 + address.hashCode()
        result = result * 31 + name.hashCode()
        return result
    }

    override fun toString(): String {
        return module + "   " + address + "     " + name
    }
}

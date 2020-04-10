package org.palliums.violascore.transaction

import org.palliums.violascore.wallet.RAW_TRANSACTION_HASH_SALT
import org.palliums.violascore.serialization.LCSInputStream
import org.palliums.violascore.serialization.LCSOutputStream
import org.palliums.violascore.transaction.storage.TypeTag
import org.spongycastle.jcajce.provider.digest.SHA3
import java.lang.RuntimeException


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

    fun toHashByteArray(): ByteArray {
        val sha3256 = SHA3.Digest256()
        sha3256.update(SHA3.Digest256().digest(RAW_TRANSACTION_HASH_SALT.toByteArray()))
        sha3256.update(toByteArray())
        return sha3256.digest()
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
        val stream = LCSOutputStream()
        stream.write(rawTxn.toByteArray())
        stream.write(authenticator.toByteArray())
        return stream.toByteArray()
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

        /**
         * data: 数据
         *@return Pair first: Address,second：AuthenticationKeyPrefix
         */
        fun convert(date: String): Pair<String, String> {
            return when (date.length) {
                32 -> {
                    Pair(date, "")
                }
                64 -> {
                    val address = date.substring(date.length / 2)
                    val authenticationKeyPrefix = date.substring(0, date.length / 2)
                    Pair(address, authenticationKeyPrefix)
                }
                else -> {
                    throw RuntimeException()
                }
            }
        }
    }
}

data class AccessPath(
    val address: AccountAddress,
    val path: ByteArray
) {
    companion object {
        fun decode(input: LCSInputStream): AccessPath {
            return AccessPath(
                AccountAddress.decode(input),
                input.readBytes()
            )
        }
    }

    fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeBytes(this.address.toByteArray())
        stream.writeBytes(this.path)
        return stream.toByteArray()
    }
}
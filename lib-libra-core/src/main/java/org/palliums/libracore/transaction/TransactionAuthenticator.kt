package org.palliums.libracore.transaction

import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.wallet.MultiEd25519PublicKey
import org.spongycastle.jcajce.provider.digest.SHA3
import org.spongycastle.util.encoders.Hex

class AuthenticationKey {
    enum class Scheme(val value: Byte) {
        Ed25519(0),
        MultiEd25519(1)
    }

    companion object {
        fun ed25519(publicKey: ByteArray): AuthenticationKey {
            return AuthenticationKey(publicKey, Scheme.Ed25519)
        }

        fun multi_ed25519(MultiEd25519PublicKey: MultiEd25519PublicKey): AuthenticationKey {
            return AuthenticationKey(MultiEd25519PublicKey, Scheme.Ed25519)
        }
    }

    private val authenticationKeyBytes: ByteArray

    constructor(publicKey: ByteArray, scheme: Scheme) {

        val schemePublicKey = publicKey.plus(scheme.value)

        val sha3256 = SHA3.Digest256()
        sha3256.update(schemePublicKey)

        this.authenticationKeyBytes = sha3256.digest()
    }

    constructor(publicKey: MultiEd25519PublicKey, scheme: Scheme) {

        val schemePublicKey = publicKey.toByteArray().plus(scheme.value)

        val sha3256 = SHA3.Digest256()
        sha3256.update(schemePublicKey)

        this.authenticationKeyBytes = sha3256.digest()
    }

    fun prefix(): ByteArray {
        return authenticationKeyBytes.copyOfRange(0, 16)
    }

    fun getShortAddress(): ByteArray {
        return authenticationKeyBytes.copyOfRange(16, 32)
    }

    fun toBytes(): ByteArray {
        return this.authenticationKeyBytes
    }

    fun toHex(): String {
        return Hex.toHexString(this.authenticationKeyBytes)
    }
}

interface TransactionAuthenticator {
    companion object {
        fun decode(input: LCSInputStream): TransactionAuthenticator {
            return when (input.readInt().toByte()) {
                AuthenticationKey.Scheme.Ed25519.value -> {
                    TransactionSignAuthenticator(input.readBytes(), input.readBytes())
                }
                AuthenticationKey.Scheme.MultiEd25519.value -> {
                    // todo
                    val xx = MultiEd25519PublicKey(arrayListOf(), 0)
                    TransactionMultiSignAuthenticator(xx, input.readBytes())
                }
                else -> {
                    TransactionSignAuthenticator(input.readBytes(), input.readBytes())
                }
            }
        }
    }

    fun toByteArray(): ByteArray
}

class TransactionSignAuthenticator(
    val publicKey: ByteArray,
    val signature: ByteArray
) : TransactionAuthenticator {
    override fun toByteArray(): ByteArray {
        println(
            "public key size:${publicKey.size} hex:${LCS.encodeInt(publicKey.size).toHex()}"
        )
        println("signature size:${signature.size} hex:${LCS.encodeInt(signature.size).toHex()}")

        val stream = LCSOutputStream()
        stream.writeInt(AuthenticationKey.Scheme.Ed25519.value.toInt())
        stream.writeBytes(publicKey)
        stream.writeBytes(signature)
        return stream.toByteArray()
    }
}

class TransactionMultiSignAuthenticator(
    private val multiPublicKey: MultiEd25519PublicKey,
    val signature: ByteArray
) : TransactionAuthenticator {
    override fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeByte(AuthenticationKey.Scheme.MultiEd25519.value)
        stream.writeBytes(multiPublicKey.toByteArray())
        stream.writeBytes(signature)
        return stream.toByteArray()
    }
}
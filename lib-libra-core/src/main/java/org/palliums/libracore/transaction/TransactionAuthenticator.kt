package org.palliums.libracore.transaction

import org.palliums.libracore.crypto.*
import org.palliums.libracore.serialization.LCS
import org.palliums.libracore.serialization.LCSInputStream
import org.palliums.libracore.serialization.LCSOutputStream
import org.palliums.libracore.serialization.toHex
import org.spongycastle.jcajce.provider.digest.SHA3
import org.spongycastle.util.encoders.Hex

class AuthenticationKey(publicKey: KeyPair.PublicKey, scheme: Scheme) {
    enum class Scheme(val value: Byte) {
        Ed25519(0),
        MultiEd25519(1)
    }

    companion object {
        fun ed25519(publicKey: Ed25519PublicKey): AuthenticationKey {
            return AuthenticationKey(publicKey, Scheme.Ed25519)
        }

        fun multiEd25519(MultiEd25519PublicKey: MultiEd25519PublicKey): AuthenticationKey {
            return AuthenticationKey(MultiEd25519PublicKey, Scheme.MultiEd25519)
        }
    }

    private val authenticationKeyBytes: ByteArray

    init {
        val schemePublicKey = publicKey.toByteArray().plus(scheme.value)
        val sha3256 = SHA3.Digest256()
        sha3256.update(schemePublicKey)
        this.authenticationKeyBytes = sha3256.digest()
    }

    fun prefix(): ByteArray {
        return authenticationKeyBytes.copyOfRange(0, 16)
    }

    fun getShortAddress(): AccountAddress {
        return AccountAddress(authenticationKeyBytes.copyOfRange(16, 32))
    }

    fun toBytes(): ByteArray {
        return this.authenticationKeyBytes
    }

    fun toHex(): String {
        return Hex.toHexString(this.authenticationKeyBytes)
    }
}

interface TransactionAuthenticator {

    fun toByteArray(): ByteArray
}

class TransactionSignAuthenticator(
    val publicKey: KeyPair.PublicKey,
    val signature: Signature
) : TransactionAuthenticator {
    override fun toByteArray(): ByteArray {
        println(
            "public key size:${publicKey.toByteArray().size} hex:${LCS.encodeInt(publicKey.toByteArray().size)
                .toHex()}"
        )
        println(
            "signature size:${signature.toByteArray().size} hex:${LCS.encodeInt(signature.toByteArray().size)
                .toHex()}"
        )

        val stream = LCSOutputStream()
        stream.writeU8(AuthenticationKey.Scheme.Ed25519.value.toInt())
        stream.writeBytes(publicKey.toByteArray())
        stream.writeBytes(signature.toByteArray())
        return stream.toByteArray()
    }
}

class TransactionMultiSignAuthenticator(
    private val multiPublicKey: MultiEd25519PublicKey,
    val signature: MultiEd25519Signature
) : TransactionAuthenticator {
    override fun toByteArray(): ByteArray {
        val stream = LCSOutputStream()
        stream.writeU8(AuthenticationKey.Scheme.MultiEd25519.value.toInt())
        stream.writeBytes(multiPublicKey.toByteArray())
        stream.writeBytes(signature.toByteArray())
        return stream.toByteArray()
    }
}
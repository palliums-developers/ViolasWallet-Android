package org.palliums.libracore.crypto

import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.palliums.libracore.serialization.toHex
import java.security.MessageDigest

open class Ed25519PrivateKey(private val key: ByteArray) : KeyPair.PrivateKey {
    override fun toByteArray(): ByteArray = key
}

class Ed25519PrivateKeyIndex(key: Ed25519PrivateKey, private val index: Int) : Ed25519PrivateKey(key.toByteArray()) {
    fun getIndex() = index
}

class Ed25519PublicKey(private val key: ByteArray) : KeyPair.PublicKey {
    override fun toByteArray(): ByteArray = key
}

open class Ed25519Signature(private val signature: ByteArray) : Signature {
    override fun toByteArray(): ByteArray = signature
}

class Ed25519KeyPair(private val secretKey: ByteArray) : KeyPair {

    companion object {
        private val mDsaNamedCurveSpec =
            EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    }

    private val mEdDSAPrivateKey by lazy {
        EdDSAPrivateKey(EdDSAPrivateKeySpec(secretKey, mDsaNamedCurveSpec))
    }
    private val mEdDSAPublicKey by lazy {
        EdDSAPublicKey(EdDSAPublicKeySpec(mEdDSAPrivateKey.a, mEdDSAPrivateKey.params))
    }

    override fun getPrivateKey(): Ed25519PrivateKey {
        return Ed25519PrivateKey(secretKey)
    }

    override fun getPublicKey(): Ed25519PublicKey {
        return Ed25519PublicKey(mEdDSAPublicKey.abyte)
    }

    override fun signMessage(message: ByteArray): Signature {
        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initSign(mEdDSAPrivateKey)
        edDSAEngine.update(message)
        return Ed25519Signature(edDSAEngine.sign())
    }

    override fun verify(signed: Signature, message: ByteArray): Boolean {
        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initVerify(mEdDSAPublicKey)
        edDSAEngine.update(message)
        return edDSAEngine.verify(signed.toByteArray())
    }
}


package org.palliums.libracore.crypto

import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest

class Ed25519KeyPair(secretKey: ByteArray) {
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

    fun getPublicKey(): ByteArray {
        return mEdDSAPublicKey.abyte
    }

    fun signMessage(message: ByteArray): ByteArray {
        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initSign(mEdDSAPrivateKey)
        edDSAEngine.update(message)
        return edDSAEngine.sign()
    }

    fun verify(signed: ByteArray, message: ByteArray): Boolean {
        val edDSAEngine = EdDSAEngine(MessageDigest.getInstance(mDsaNamedCurveSpec.hashAlgorithm))
        edDSAEngine.initVerify(mEdDSAPublicKey)
        edDSAEngine.update(message)
        return edDSAEngine.verify(signed)
    }
}


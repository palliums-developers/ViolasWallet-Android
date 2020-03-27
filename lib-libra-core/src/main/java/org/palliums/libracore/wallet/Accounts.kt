package org.palliums.libracore.wallet

import org.palliums.libracore.serialization.toHex
import org.spongycastle.jcajce.provider.digest.SHA3
import org.spongycastle.util.encoders.Hex

/**
 * Created by elephant on 2019-09-20 11:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

class Account {

    val keyPair: KeyPair
    private var address: AccountAddress? = null
    private var authenticationKey: AuthenticationKey? = null

    constructor(keyPair: KeyPair) {
        this.keyPair = keyPair
    }

    fun getAddress(): AccountAddress {
        if (address == null) {
            val authenticationKey = authenticationKey ?: getAuthenticationKey()
            this.address = AccountAddress(authenticationKey.getShortAddress())
        }

        return this.address!!
    }

    fun getAuthenticationKey(): AuthenticationKey {
        if (this.authenticationKey == null) {
            this.authenticationKey = AuthenticationKey.ed25519(this.keyPair.getPublicKey())
        }
        return this.authenticationKey!!
    }

    fun getPublicKey(): String {
        return keyPair.getPublicKey().toHex()
    }
}

class AuthenticationKey {
    enum class Scheme(val value: Byte) {
        Ed25519(0),
        MultiEd25519(1)
    }

    companion object {
        fun ed25519(publicKey: ByteArray): AuthenticationKey {
            return AuthenticationKey(publicKey, Scheme.Ed25519)
        }

//        fun multi_ed25519(MultiEd25519PublicKey: MultiEd25519PublicKey): AuthenticationKey {
//            return AuthenticationKey(publicKey, Scheme.Ed25519)
//        }
    }

    private val authenticationKeyBytes: ByteArray

    constructor(publicKey: ByteArray, scheme: Scheme) {

        val schemePublicKey = publicKey.plus(scheme.value)

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

class AccountAddress {

    private val addressBytes: ByteArray

    constructor(address: ByteArray) {
        this.addressBytes = address
    }

    fun toBytes(): ByteArray {
        return this.addressBytes
    }

    fun toHex(): String {
        return Hex.toHexString(this.addressBytes)
    }
}
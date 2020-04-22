package org.palliums.libracore.wallet

import org.palliums.libracore.crypto.Ed25519PublicKey
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.crypto.MultiEd25519PublicKey
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.AuthenticationKey
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
            this.authenticationKey = when (val publicKey = this.keyPair.getPublicKey()) {
                is Ed25519PublicKey -> AuthenticationKey.ed25519(publicKey)
                is MultiEd25519PublicKey -> AuthenticationKey.multiEd25519(publicKey)
                else -> {
                    TODO("error")
                }
            }
        }
        return this.authenticationKey!!
    }

    fun getPublicKey(): String {
        return keyPair.getPublicKey().toByteArray().toHex()
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
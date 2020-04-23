package org.palliums.libracore.wallet

import org.palliums.libracore.crypto.Ed25519PublicKey
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.crypto.MultiEd25519PublicKey
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.AuthenticationKey

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
            this.address = authenticationKey.getShortAddress()
        }

        return this.address!!
    }

    fun getAuthenticationKey(): AuthenticationKey {
        return this.authenticationKey ?: synchronized(this) {
            when (val publicKey = this.keyPair.getPublicKey()) {
                is Ed25519PublicKey -> AuthenticationKey.ed25519(publicKey)
                is MultiEd25519PublicKey -> AuthenticationKey.multiEd25519(publicKey)
                else -> {
                    TODO("error")
                }
            }.also {
                this.authenticationKey = it
            }
        }
    }

    fun getPublicKey(): String {
        return keyPair.getPublicKey().toByteArray().toHex()
    }
}
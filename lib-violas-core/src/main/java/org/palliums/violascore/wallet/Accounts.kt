package org.palliums.violascore.wallet

import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.AuthenticationKey
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
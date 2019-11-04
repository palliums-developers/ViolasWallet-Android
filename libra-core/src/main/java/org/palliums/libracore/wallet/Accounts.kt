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

    constructor(keyPair: KeyPair) {
        this.keyPair = keyPair
    }

    fun getAddress(): AccountAddress {
        if (this.address == null) {
            val sha3256 = SHA3.Digest256()
            sha3256.update(this.keyPair.getPublicKey())
            this.address = AccountAddress(sha3256.digest())
        }

        return this.address!!
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
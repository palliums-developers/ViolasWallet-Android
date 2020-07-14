package com.violas.wallet.biz.exchangeMapping

import org.palliums.libracore.serialization.hexToBytes

// ==== Account Interface ======/
interface BTCAccount {
    fun getAddress(): String
    fun getPublicKey(): ByteArray
}

interface LibraToken {
    fun getAddress(): ByteArray
    fun getTokenName(): String
    fun getTokenMark(): LibraTokenMark
}

interface ViolasToken {
    fun getAddress(): ByteArray
    fun getTokenName(): String
    fun getTokenMark(): LibraTokenMark
}

abstract class MappingAccount(private val sendPrivateKey: ByteArray? = null) {
    fun getPrivateKey() = sendPrivateKey

    fun isSendAccount() = getPrivateKey() != null
}

class BTCMappingAccount(
    private val publicKey: ByteArray,
    private val BTCAddress: String,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey),
    BTCAccount {
    override fun getAddress() = BTCAddress

    override fun getPublicKey() = publicKey
}

class LibraMappingToken(
    private val address: String,
    private val tokenName: String,
    private val tokenMark: LibraTokenMark,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey),
    LibraToken {
    override fun getAddress() = address.hexToBytes()
    override fun getTokenName() = tokenName
    override fun getTokenMark() = tokenMark
}

class ViolasMappingToken(
    private val address: String,
    private val tokenName: String,
    private val tokenMark: LibraTokenMark,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey),
    ViolasToken {
    override fun getAddress() = address.hexToBytes()
    override fun getTokenName() = tokenName

    override fun getTokenMark() = tokenMark
}
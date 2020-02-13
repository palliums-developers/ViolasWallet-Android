package com.violas.wallet.biz.exchangeMapping

import org.palliums.libracore.serialization.hexToBytes

// ==== Account Interface ======/
interface BTCAccount {
    fun getAddress(): String
    fun getPublicKey(): ByteArray
}

interface LibraAccount {
    fun getAddress(): ByteArray
}

interface ViolasAccount {
    fun getAddress(): ByteArray
    fun getTokenAddress(): ByteArray
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

class LibraMappingAccount(
    private val address: String,
    privateKey: ByteArray? = null

) : MappingAccount(privateKey),
    LibraAccount {
    override fun getAddress() = address.hexToBytes()
}

class ViolasMappingAccount(
    private val address: String,
    private val tokenAddress: String,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey),
    ViolasAccount {
    override fun getAddress() = address.hexToBytes()
    override fun getTokenAddress() = tokenAddress.hexToBytes()
}
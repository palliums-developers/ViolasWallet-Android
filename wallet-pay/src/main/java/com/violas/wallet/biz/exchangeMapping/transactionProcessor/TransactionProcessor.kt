package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import com.violas.wallet.biz.exchangeMapping.MappingAccount
import java.math.BigDecimal

interface TransactionProcessor {
    fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean

    @Throws(Exception::class)
    suspend fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String
}
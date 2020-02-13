package com.violas.wallet.biz.exchangeMapping

import java.math.BigDecimal

interface ExchangePair {
    fun getFirst(): ExchangeAssert
    fun getLast(): ExchangeAssert
    fun getRate(): BigDecimal
    fun getReceiveFirstAddress(): String
    fun getReceiveLastAddress(): String
}

class ExchangePairManager() {
    private val mExchangePair = ArrayList<ExchangePair>()

    fun addExchangePair(pair: ExchangePair) {
        mExchangePair.add(pair)
    }

    fun findExchangePair(coinNumber: Int): ExchangePair? {
        mExchangePair.forEach {
            if (it.getFirst().getCoinType().coinType() == coinNumber) {
                return it
            }
        }
        return null
    }
}
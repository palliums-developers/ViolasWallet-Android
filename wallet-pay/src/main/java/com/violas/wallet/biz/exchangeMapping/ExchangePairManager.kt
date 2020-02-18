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

    fun findExchangePair(coinNumber: Int, forward: Boolean): List<ExchangePair> {
        val pairs = arrayListOf<ExchangePair>()
        mExchangePair.forEach {
            val coin = if (forward) {
                it.getFirst()
            } else {
                it.getLast()
            }
            if (coin.getCoinType().coinType() == coinNumber) {
                pairs.add(it)
            }
        }
        return pairs
    }
}
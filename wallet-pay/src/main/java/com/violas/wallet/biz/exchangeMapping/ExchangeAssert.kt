package com.violas.wallet.biz.exchangeMapping

import com.quincysx.crypto.CoinTypes

interface ExchangeAssert {
    fun getCoinType(): CoinTypes

    fun getName(): String {
        return getCoinType().coinName()
    }
}

interface ExchangeCoin : ExchangeAssert {

}

interface ExchangeToken : ExchangeAssert {
    fun getTokenIdx(): Long
}

class ExchangeTokenImpl(
    private val coinTypes: CoinTypes,
    private val tokenName: String,
    private val tokenIdx: Long
) :
    ExchangeToken {
    override fun getTokenIdx(): Long {
        return tokenIdx
    }

    override fun getCoinType(): CoinTypes {
        return coinTypes
    }

    override fun getName(): String {
        return tokenName
    }
}

class ExchangeCoinImpl(private val coinTypes: CoinTypes) : ExchangeCoin {
    override fun getCoinType(): CoinTypes {
        return coinTypes
    }
}
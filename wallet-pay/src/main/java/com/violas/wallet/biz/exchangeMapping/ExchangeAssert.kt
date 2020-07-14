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

data class LibraTokenMark(
    val address: String,
    val module: String,
    val name: String
)

interface ExchangeLibraToken : ExchangeAssert {
    fun getTokenMark(): LibraTokenMark
}

class ExchangeLibraTokenImpl(
    private val coinTypes: CoinTypes,
    private val tokenName: String,
    private val tokenIdx: LibraTokenMark
) :
    ExchangeLibraToken {
    override fun getTokenMark(): LibraTokenMark {
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
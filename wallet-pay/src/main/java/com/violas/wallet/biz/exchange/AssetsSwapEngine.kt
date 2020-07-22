package com.violas.wallet.biz.exchange

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.processor.IProcessor
import com.violas.wallet.ui.main.market.bean.ITokenVo
import org.palliums.libracore.http.LibraException
import org.palliums.violascore.http.ViolasException
import java.lang.RuntimeException
import java.util.*

// 收款地址未激活
class AccountPayeeNotFindException : RuntimeException()

// 该本班无法处理的交易
class UnsupportedTradingPairsException : RuntimeException()

// 收款地址 Token 未激活
class AccountPayeeTokenNotActiveException(
    val coinTypes: CoinTypes,
    val address: String,
    val assetsMark: ITokenVo
) :
    RuntimeException()

class AccountNotFindAddressException : RuntimeException()

internal class AssetsSwapEngine {
    private val processors = LinkedList<IProcessor>()

    fun clearProcessor() {
        processors.clear()
    }

    fun addProcessor(processor: IProcessor) {
        if (!processors.contains(processor)) {
            processors.add(processor)
        }
    }

    @Throws(
        LibraException::class,
        ViolasException::class,
        AccountPayeeNotFindException::class,
        AccountPayeeTokenNotActiveException::class,
        UnsupportedTradingPairsException::class
    )
    suspend fun swap(
        pwd: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ): String {
        processors.forEach {
            if (it.hasHandle(tokenFrom, tokenTo)) {
                return it.handle(
                    pwd,
                    tokenFrom,
                    tokenTo,
                    payee,
                    amountIn,
                    amountOutMin,
                    path,
                    data
                )
            }
        }
        throw UnsupportedTradingPairsException()
    }
}
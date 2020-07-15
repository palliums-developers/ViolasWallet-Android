package com.violas.wallet.biz.exchange

import com.violas.wallet.biz.exchange.processor.*
import com.violas.wallet.ui.main.market.bean.ITokenVo
import java.lang.RuntimeException
import java.util.*

// 收款地址未激活
class AccountPayeeNotFindException : RuntimeException()

// 收款地址 Token 未激活
class AccountPayeeTokenNotActiveException : RuntimeException()

internal class AssetsSwapEngine {
    private val processors = LinkedList<IProcessor>()

    init {
        processors.add(ViolasTokenToViolasTokenProcessor())
        processors.add(ViolasToAssetsMappingProcessor())
        processors.add(LibraToMappingAssetsProcessor())
        processors.add(BTCToMappingAssetsProcessor())
    }

    suspend fun swap(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ) {
        processors.forEach {
            if (it.hasHandle(tokenFrom, tokenTo)) {
                it.handle(privateKey, tokenFrom, tokenTo, payee, amountIn, amountOutMin, path, data)
            }
        }
    }
}
package com.violas.wallet.biz.exchange

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.processor.IProcessor
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.ITokenVo
import org.palliums.libracore.http.LibraException
import org.palliums.violascore.http.ViolasException
import java.lang.RuntimeException
import java.util.*

// 收款地址未激活
class AccountPayeeNotFindException : RuntimeException()

// 收款地址 Token 未激活
class AccountPayeeTokenNotActiveException(val coinTypes: CoinTypes,val address:String, val assetsMark: ITokenVo) :
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
        AccountPayeeTokenNotActiveException::class
    )
    suspend fun swap(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
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
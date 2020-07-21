package com.violas.wallet.biz.exchange.processor

import androidx.annotation.WorkerThread
import com.violas.wallet.ui.main.market.bean.ITokenVo

interface IProcessor {
    fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean

    @WorkerThread
    suspend fun handle(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ): String
}
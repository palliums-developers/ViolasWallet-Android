package com.violas.wallet.biz.exchange.processor

import androidx.annotation.WorkerThread
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.ui.main.market.bean.ITokenVo

interface IProcessor {
    fun hasHandleSwap(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean

    @WorkerThread
    suspend fun handle(
        pwd: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ): String

    fun hasHandleCancel(
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark
    ): Boolean

    @WorkerThread
    suspend fun cancel(
        pwd: ByteArray,
        fromIAssetMark: IAssetMark,
        toIAssetMark: IAssetMark,
        typeTag: String,
        originPayeeAddress: String,
        tranId: String?,
        sequence: String?
    ): String
}
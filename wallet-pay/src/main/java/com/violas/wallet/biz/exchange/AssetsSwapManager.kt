package com.violas.wallet.biz.exchange

import com.violas.wallet.ui.main.market.bean.ITokenVo

class AssetsSwapManager {
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
        AssetsSwapEngine().swap(
            privateKey,
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
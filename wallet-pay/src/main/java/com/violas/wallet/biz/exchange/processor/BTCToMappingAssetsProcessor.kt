package com.violas.wallet.biz.exchange.processor

import com.violas.wallet.ui.main.market.bean.ITokenVo

class BTCToMappingAssetsProcessor : IProcessor {
    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return false
    }

    override suspend fun handle(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ) {
        TODO("Not yet implemented")
    }
}
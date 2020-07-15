package com.violas.wallet.biz.exchange.processor

import com.violas.wallet.ui.main.market.bean.ITokenVo

class LibraToMappingAssetsProcessor : IProcessor {
    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        TODO("Not yet implemented")
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
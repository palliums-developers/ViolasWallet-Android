package com.violas.wallet.biz.exchange.processor

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.MappingInfo
import com.violas.wallet.common.Vm
import com.violas.wallet.ui.main.market.bean.*

class BTCToMappingAssetsProcessor(
    private val supportMappingPair: HashMap<String, MappingInfo>
) : IProcessor {
    override fun hasHandle(tokenFrom: ITokenVo, tokenTo: ITokenVo): Boolean {
        return tokenFrom is PlatformTokenVo
                && tokenFrom.coinNumber == if (Vm.TestNet) {
            CoinTypes.BitcoinTest
        } else {
            CoinTypes.Bitcoin
        }.coinType()
                && supportMappingPair.containsKey(IAssetsMark.convert(tokenTo).mark())
    }

    override suspend fun handle(
        privateKey: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        payee: String?,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ) {

    }
}
package com.violas.wallet.ui.main.market.bean

import com.quincysx.crypto.CoinType
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import java.lang.RuntimeException

interface IAssetsMark {
    companion object {
        fun convert(iTokenVo: ITokenVo): IAssetsMark {
            return if (iTokenVo is PlatformTokenVo) {
                CoinAssetsMark(CoinType.parseCoinNumber(iTokenVo.coinNumber))
            } else if (iTokenVo is StableTokenVo) {
                LibraTokenAssetsMark(
                    CoinType.parseCoinNumber(iTokenVo.coinNumber),
                    iTokenVo.module,
                    iTokenVo.address,
                    iTokenVo.name
                )
            } else {
                throw RuntimeException("不支持的")
            }
        }

        fun convert(iTokenVo: AssetsVo): IAssetsMark {
            return if (iTokenVo is AssetsCoinVo) {
                CoinAssetsMark(CoinType.parseCoinNumber(iTokenVo.getCoinNumber()))
            } else if (iTokenVo is AssetsTokenVo ) {
                LibraTokenAssetsMark(
                    CoinType.parseCoinNumber(iTokenVo.getCoinNumber()),
                    iTokenVo.module,
                    iTokenVo.address,
                    iTokenVo.name
                )
            }else if(iTokenVo is HiddenTokenVo){
                LibraTokenAssetsMark(
                    CoinType.parseCoinNumber(iTokenVo.getCoinNumber()),
                    iTokenVo.module,
                    iTokenVo.address,
                    iTokenVo.name
                )
            }else {
                throw RuntimeException("不支持的币种")
            }
        }
    }

    fun mark(): String

    fun coinNumber(): Int
}

class CoinAssetsMark(val coinType: CoinType) : IAssetsMark {
    override fun mark(): String {
        return "c${coinType.coinNumber()}"
    }

    override fun coinNumber(): Int {
        return coinType.coinNumber()
    }

    override fun equals(other: Any?): Boolean {
        return (other is CoinAssetsMark) && coinType == other.coinType
    }
}

class LibraTokenAssetsMark(
    val coinType: CoinType,
    val module: String,
    val address: String,
    val name: String
) : IAssetsMark {
    override fun mark(): String {
        return "lt${coinType.coinNumber()}${module}${address}${name}"
    }

    override fun coinNumber(): Int {
        return coinType.coinNumber()
    }

    override fun equals(other: Any?): Boolean {
        return (other is LibraTokenAssetsMark)
                && coinType == other.coinType
                && module == other.module
                && address == other.address
                && name == other.name
    }
}
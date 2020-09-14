package com.violas.wallet.ui.main.market.bean

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo
import java.lang.RuntimeException

interface IAssetsMark {
    companion object {
        fun convert(iTokenVo: ITokenVo): IAssetsMark {
            return if (iTokenVo is PlatformTokenVo) {
                CoinAssetsMark(CoinTypes.parseCoinType(iTokenVo.coinNumber))
            } else if (iTokenVo is StableTokenVo) {
                LibraTokenAssetsMark(
                    CoinTypes.parseCoinType(iTokenVo.coinNumber),
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
                CoinAssetsMark(CoinTypes.parseCoinType(iTokenVo.getCoinNumber()))
            } else if (iTokenVo is AssetsTokenVo ) {
                LibraTokenAssetsMark(
                    CoinTypes.parseCoinType(iTokenVo.getCoinNumber()),
                    iTokenVo.module,
                    iTokenVo.address,
                    iTokenVo.name
                )
            }else if(iTokenVo is HiddenTokenVo){
                LibraTokenAssetsMark(
                    CoinTypes.parseCoinType(iTokenVo.getCoinNumber()),
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

class CoinAssetsMark(val coinTypes: CoinTypes) : IAssetsMark {
    override fun mark(): String {
        return "c${coinTypes.coinType()}"
    }

    override fun coinNumber(): Int {
        return coinTypes.coinType()
    }

    override fun equals(other: Any?): Boolean {
        return (other is CoinAssetsMark) && coinTypes == other.coinTypes
    }
}

class LibraTokenAssetsMark(
    val coinTypes: CoinTypes,
    val module: String,
    val address: String,
    val name: String
) : IAssetsMark {
    override fun mark(): String {
        return "lt${coinTypes.coinType()}${module}${address}${name}"
    }

    override fun coinNumber(): Int {
        return coinTypes.coinType()
    }

    override fun equals(other: Any?): Boolean {
        return (other is LibraTokenAssetsMark)
                && coinTypes == other.coinTypes
                && module == other.module
                && address == other.address
                && name == other.name
    }
}
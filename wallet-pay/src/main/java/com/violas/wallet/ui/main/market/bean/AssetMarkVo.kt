package com.violas.wallet.ui.main.market.bean

import com.quincysx.crypto.CoinType
import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.viewModel.bean.AssetVo
import com.violas.wallet.viewModel.bean.CoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
import java.lang.RuntimeException

interface IAssetMark {

    companion object {

        fun convert(token: ITokenVo): IAssetMark {
            return when (token) {
                is PlatformTokenVo -> {
                    CoinAssetMark(CoinType.parseCoinNumber(token.coinNumber))
                }
                is StableTokenVo -> {
                    DiemCurrencyAssetMark(
                        CoinType.parseCoinNumber(token.coinNumber),
                        token.module,
                        token.address,
                        token.name
                    )
                }
            }
        }

        fun convert(asset: AssetVo): IAssetMark {
            return when (asset) {
                is CoinAssetVo -> {
                    CoinAssetMark(CoinType.parseCoinNumber(asset.getCoinNumber()))
                }
                is DiemCurrencyAssetVo -> {
                    DiemCurrencyAssetMark(
                        CoinType.parseCoinNumber(asset.getCoinNumber()),
                        asset.currency.module,
                        asset.currency.address,
                        asset.currency.name
                    )
                }
            }
        }

        fun convert(coinNumber: Int, currency: DiemCurrency? = null): IAssetMark {
            val coinType = CoinType.parseCoinNumber(coinNumber)
            return when {
                coinType == getBitcoinCoinType() -> {
                    CoinAssetMark(coinType)
                }
                (coinType == getViolasCoinType() || coinType == getDiemCoinType()) && currency != null -> {
                    DiemCurrencyAssetMark(
                        coinType,
                        currency.module,
                        currency.address,
                        currency.name
                    )
                }
                else -> {
                    throw RuntimeException("Unsupported coin number $coinNumber")
                }
            }
        }
    }

    fun mark(): String

    fun coinNumber(): Int
}

class CoinAssetMark(val coinType: CoinType) : IAssetMark {
    override fun mark(): String {
        return "c${coinType.coinNumber()}"
    }

    override fun coinNumber(): Int {
        return coinType.coinNumber()
    }

    override fun equals(other: Any?): Boolean {
        return (other is CoinAssetMark) && coinType == other.coinType
    }
}

class DiemCurrencyAssetMark(
    val coinType: CoinType,
    val module: String,
    val address: String,
    val name: String
) : IAssetMark {
    override fun mark(): String {
        return "dc${coinType.coinNumber()}${module}${name}${address}"
    }

    override fun coinNumber(): Int {
        return coinType.coinNumber()
    }

    override fun equals(other: Any?): Boolean {
        return (other is DiemCurrencyAssetMark)
                && coinType == other.coinType
                && module == other.module
                && address == other.address
                && name == other.name
    }
}
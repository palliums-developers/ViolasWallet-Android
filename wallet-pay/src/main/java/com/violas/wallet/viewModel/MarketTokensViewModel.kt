package com.violas.wallet.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.PlatformTokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo

/**
 * Created by elephant on 2020/7/8 13:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易市场支持的Token列表
 */
class MarketTokensViewModel : BaseViewModel() {

    private val marketTokensLiveData = MutableLiveData<List<ITokenVo>?>()

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val vls = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
            coinNumber = CoinTypes.Violas.coinType(),
            displayName = "VLS",
            logoUrl = "",
            amount = 100_000000,
            anchorValue = 1.00,
            selected = false
        )

        val vlsusd = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSUSD",
            name = "VLSUSD",
            displayName = "VLSUSD",
            logoUrl = "",
            localEnable = true,
            chainEnable = true,
            amount = 200_000000,
            anchorValue = 1.00,
            selected = false
        )

        val vlsgbp = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSGBP",
            name = "VLSGBP",
            displayName = "VLSGBP",
            logoUrl = "",
            localEnable = true,
            chainEnable = true,
            amount = 300_000000,
            anchorValue = 1.2526,
            selected = false
        )

        val vlseur = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSEUR",
            name = "VLSEUR",
            displayName = "VLSEUR",
            logoUrl = "",
            localEnable = true,
            chainEnable = true,
            amount = 400_000000,
            anchorValue = 1.1272,
            selected = false
        )

        val vlssgd = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSSGD",
            name = "VLSSGD",
            displayName = "VLSSGD",
            logoUrl = "",
            localEnable = true,
            chainEnable = true,
            amount = 500_000000,
            anchorValue = 0.7167,
            selected = false
        )

        val vlsjpy = StableTokenVo(
            accountDoId = 0,
            coinNumber = CoinTypes.Violas.coinType(),
            marketIndex = 0,
            tokenDoId = 0,
            address = "00000000000000000000000000000000",
            module = "VLSJPY",
            name = "VLSJPY",
            displayName = "VLSJPY",
            logoUrl = "",
            localEnable = true,
            chainEnable = false,
            amount = 500_000000,
            anchorValue = 0.0093,
            selected = false
        )

        val lbr = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
            coinNumber = CoinTypes.Libra.coinType(),
            displayName = "LBR",
            logoUrl = "",
            amount = 100_000000,
            anchorValue = 1.00,
            selected = false
        )

        val btc = PlatformTokenVo(
            accountDoId = 0,
            accountType = AccountType.Normal,
            accountAddress = "00000000000000000000000000000000",
            coinNumber = CoinTypes.BitcoinTest.coinType(),
            displayName = "BTC",
            logoUrl = "",
            amount = 100_000000,
            anchorValue = 9000.00,
            selected = false
        )

        val list = mutableListOf(
            vls, vlsusd, vlsgbp, vlseur, vlssgd, vlsjpy, lbr, btc
        )
        marketTokensLiveData.postValue(list)
    }

    fun getMarketSupportTokensLiveData(): LiveData<List<ITokenVo>?> {
        return marketTokensLiveData
    }
}
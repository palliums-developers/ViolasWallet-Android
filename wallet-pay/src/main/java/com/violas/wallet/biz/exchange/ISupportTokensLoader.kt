package com.violas.wallet.biz.exchange

import com.violas.wallet.ui.main.market.bean.ITokenVo

interface ISupportTokensLoader {
    fun load(): List<ITokenVo>
}

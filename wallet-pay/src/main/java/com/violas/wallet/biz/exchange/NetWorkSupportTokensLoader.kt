package com.violas.wallet.biz.exchange

import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.ui.main.market.bean.ITokenVo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class NetWorkSupportTokensLoader : ISupportTokensLoader {
    private val mExchangeManager by lazy {
        ExchangeManager()
    }

    override fun load(): List<ITokenVo> {
        val result = ArrayList<ITokenVo>()
        val countDownLatch = CountDownLatch(1)
        GlobalScope.launch(SupervisorJob()) {
            try {
                result.addAll(mExchangeManager.getMarketSupportTokens())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return result
    }

}


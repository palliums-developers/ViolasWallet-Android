package com.violas.wallet.repository.subscribeHub

import androidx.annotation.WorkerThread
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.viewModel.bean.AssetsVo

abstract class BalanceSubscriber(private var assetsMark: IAssetsMark?) {

    private var callBack: NoticeSubscriberCallBack? = null

    fun getAssetsMarkUnique() = assetsMark?.mark() ?: ""

    fun getAssetsMark() = assetsMark

    @WorkerThread
    fun changeSubscriber(assetsMark: IAssetsMark?) {
        this.assetsMark = assetsMark
        callBack?.notice(this)
    }

    fun setNoticeSubscriberCallBack(callBack: NoticeSubscriberCallBack?) {
        this.callBack = callBack
    }

    abstract fun onNotice(assets: AssetsVo?)
}
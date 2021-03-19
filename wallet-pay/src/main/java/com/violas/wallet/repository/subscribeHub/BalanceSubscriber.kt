package com.violas.wallet.repository.subscribeHub

import androidx.annotation.WorkerThread
import com.violas.wallet.ui.main.market.bean.IAssetMark
import com.violas.wallet.viewModel.bean.AssetVo

abstract class BalanceSubscriber(private var assetMark: IAssetMark?) {

    private var callBack: NoticeSubscriberCallBack? = null

    fun getAssetsMarkUnique() = assetMark?.mark() ?: ""

    fun getAssetsMark() = assetMark

    @WorkerThread
    fun changeSubscriber(assetMark: IAssetMark?) {
        this.assetMark = assetMark
        callBack?.notice(this)
    }

    fun setNoticeSubscriberCallBack(callBack: NoticeSubscriberCallBack?) {
        this.callBack = callBack
    }

    abstract fun onNotice(asset: AssetVo?)
}
package com.violas.wallet.repository.subscribeHub

import android.util.Log
import androidx.lifecycle.*
import com.palliums.utils.CustomIOScope
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinType
import com.violas.wallet.BuildConfig
import com.violas.wallet.ui.main.market.bean.CoinAssetMark
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.CoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo
import com.violas.wallet.viewModel.bean.AssetVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface RemoveSubscriberCallBack {
    fun remove(subscriber: BalanceSubscriber)
}

interface NoticeSubscriberCallBack {
    fun notice(subscriber: BalanceSubscriber? = null)
}

object BalanceSubscribeHub : LifecycleOwner, LifecycleObserver, RemoveSubscriberCallBack,
    NoticeSubscriberCallBack, CoroutineScope by CustomIOScope() {
    private val mLifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    private val mBalanceSubscribers = hashMapOf<BalanceSubscriber, WrapperBalanceSubscriber>()
    private var mAssetMap = mapOf<String, AssetVo>()

    init {
        WalletAppViewModel.getInstance().mAssetsLiveData.observe(this) {
            launch {
                mAssetMap = it.toMap { asset ->
                    when (asset) {
                        is CoinAssetVo -> {
                            CoinAssetMark(CoinType.parseCoinNumber(asset.getCoinNumber())).mark()
                        }
                        is DiemCurrencyAssetVo -> {
                            DiemCurrencyAssetMark(
                                CoinType.parseCoinNumber(asset.getCoinNumber()),
                                asset.currency.module,
                                asset.currency.address,
                                asset.currency.name
                            ).mark()
                        }
                    }
                }
                notice()
            }
        }
        mLifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /**
     * 触发通知
     */
    override fun notice(subscriber: BalanceSubscriber?) {
        if (subscriber != null) {
            val assetsVo = mAssetMap[subscriber.getAssetsMarkUnique()]
            subscriber.onNotice(assetsVo)
        } else {
            mBalanceSubscribers.keys.forEach { item ->
                mAssetMap[item.getAssetsMarkUnique()].let { assets ->
                    item.onNotice(assets)
                }
            }
        }
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    /**
     * 发起订阅
     */
    fun observe(owner: LifecycleOwner, subscriber: BalanceSubscriber) = launch {
        subscriber.setNoticeSubscriberCallBack(this@BalanceSubscribeHub)
        val wrapperBalanceSubscriber = WrapperBalanceSubscriber(
            subscriber,
            this@BalanceSubscribeHub,
            this@BalanceSubscribeHub
        )
        owner.lifecycle.addObserver(wrapperBalanceSubscriber)
        mBalanceSubscribers[subscriber] = wrapperBalanceSubscriber
        notice(subscriber)
    }

    /**
     * 一般情况不要用主动调用
     */
    override fun remove(subscriber: BalanceSubscriber) {
        launch {
            subscriber.setNoticeSubscriberCallBack(null)
            mBalanceSubscribers.remove(subscriber)
        }
    }

    fun checkSubscriber() = launch {
        Log.d("BalanceSubscribeHub", "============================================")
        Log.d("BalanceSubscribeHub", "订阅数量:${mBalanceSubscribers.keys.size}")
        mBalanceSubscribers.keys.forEach {
            Log.d("BalanceSubscribeHub", it.toString())
        }
        Log.d("BalanceSubscribeHub", "============================================")
    }

    internal class WrapperBalanceSubscriber(
        private val subscriber: BalanceSubscriber,
        private val removeSubscriberCallBack: RemoveSubscriberCallBack,
        private val noticeSubscriberCallBack: NoticeSubscriberCallBack
    ) : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            if (BuildConfig.DEBUG) {
                checkSubscriber()
            }
            noticeSubscriberCallBack.notice(subscriber)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeSubscriberCallBack.remove(subscriber)
            if (BuildConfig.DEBUG) {
                checkSubscriber()
            }
        }
    }
}
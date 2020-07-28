package com.violas.wallet.repository.subscribeHub

import android.os.Build
import android.util.Log
import androidx.lifecycle.*
import com.palliums.utils.CustomIOScope
import com.palliums.utils.CustomMainScope
import com.palliums.utils.toMap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private var mAssetsMap = mapOf<String, AssetsVo>()

    init {
        WalletAppViewModel.getViewModelInstance().mAssetsListLiveData.observe(this, Observer {
            launch {
                mAssetsMap = it.toMap { assets ->
                    when (assets) {
                        is AssetsCoinVo -> {
                            CoinAssetsMark(CoinTypes.parseCoinType(assets.getCoinNumber())).mark()
                        }
                        is AssetsTokenVo -> {
                            LibraTokenAssetsMark(
                                CoinTypes.parseCoinType(assets.getCoinNumber()),
                                assets.module,
                                assets.address,
                                assets.name
                            ).mark()
                        }
                        else -> {
                            ""
                        }
                    }
                }
                notice()
            }
        })
        mLifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /**
     * 触发通知
     */
    override fun notice(subscriber: BalanceSubscriber?) {
        if (subscriber != null) {
            mAssetsMap[subscriber.getAssetsMark()]?.let { assets ->
                subscriber.onNotice(assets)
            }
        } else {
            mBalanceSubscribers.keys.forEach { item ->
                mAssetsMap[item.getAssetsMark()]?.let { assets ->
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
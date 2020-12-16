package com.violas.wallet.ui.incentive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.lzyzsd.jsbridge.CallBackFunction
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.palliums.extensions.logInfo
import com.palliums.utils.start
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.base.BaseBridgeWebActivity
import com.violas.wallet.common.EXTRA_KEY_TITLE
import com.violas.wallet.common.EXTRA_KEY_URL
import com.violas.wallet.event.*
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.incentive.earningsDetails.IncentiveEarningsDetailsActivity
import com.violas.wallet.ui.incentive.receiveRewards.ReceiveIncentiveRewardsActivity
import com.violas.wallet.ui.main.market.pool.MarketPoolOpMode
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.activity_bridge_web.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.random.Random

/**
 * Created by elephant on 12/15/20 4:19 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 挖矿激励web页面
 */
class IncentiveWebActivity() : BaseBridgeWebActivity() {

    companion object {
        private const val TAG = "IncentiveWebActivity"

        @JvmStatic
        private fun start(context: Context, url: String, title: String? = null) {
            Intent(context, WebCommonActivity::class.java).also {
                it.putExtra(EXTRA_KEY_URL, url)
                if (title != null) {
                    it.putExtra(EXTRA_KEY_TITLE, title)
                }
            }.start(context)
        }

        private fun getLanguageCode(): String {
            val languageLocale = MultiLanguageUtility.getInstance().languageLocale
            return if (languageLocale.language == "zh") {
                "zh"
            } else {
                "en"
            }
        }

        private suspend fun getViolasAddress(): String? = withContext(Dispatchers.IO) {
            val accountManager = WalletAppViewModel.getViewModelInstance().mAccountManager
            return@withContext try {
                accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())?.address
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 打开激励挖矿首页
         */
        suspend fun startIncentiveHomePage(context: Context) {
            val url = "http://www.baidu.com?language=${
                getLanguageCode()
            }&address=${getViolasAddress() ?: ""}"
            start(context, url)
        }

        /**
         * 打开邀请好友首页
         */
        suspend fun startInviteHomePage(context: Context) {
            val url = "http://www.baidu.com?language=${
                getLanguageCode()
            }&address=${getViolasAddress() ?: ""}"
            start(context, url)
        }

        /**
         * 打开激励挖矿规则页面
         */
        fun startIncentiveRules(context: Context) {
            val url = "http://www.baidu.com?language=${getLanguageCode()}"
            start(context, url)
        }
    }

    private val mTitle by lazy { intent.getStringExtra(EXTRA_KEY_TITLE) }
    private val mUrl by lazy { intent.getStringExtra(EXTRA_KEY_URL) }
    private val mGson by lazy { Gson() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceiveIncentiveRewardsEvent(event: ReceiveIncentiveRewardsEvent) {
        startLoad()
    }

    override fun getUrl(): String {
        return mUrl
    }

    override fun getFixedTitle(): String? {
        return mTitle
    }

    private fun <T> Response<T>.toJson(): String {
        return mGson.toJson(this)
    }

    override fun initWebView() {
        super.initWebView()
        vWeb.registerHandler("callNative") { data, callbackFunction ->
            logInfo(TAG) { "receive js request, data = $data" }

            launch(Dispatchers.Main) {
                val request = withContext(Dispatchers.IO) { mGson.fromJson<Request>(data) }

                when (request.method) {
                    "withdraw_pool_profit" -> {
                        withdrawPoolEarnings(request, callbackFunction)
                    }

                    "withdraw_bank_profit" -> {
                        withdrawBankEarnings(request, callbackFunction)
                    }

                    "new_user_check" -> {
                        startReceiveRewardsPage(request, callbackFunction)
                    }

                    "pool_farming" -> {
                        gotoHomePoolPage(request, callbackFunction)
                    }

                    "bank_deposit_farming" -> {
                        gotoHomeBankPage(request, callbackFunction, true)
                    }

                    "bank_loan_farming" -> {
                        gotoHomeBankPage(request, callbackFunction, false)
                    }

                    "mine_invite" -> {
                        startEarningsDetailsPage(request, callbackFunction)
                    }

                    "closePage" -> {
                        callbackFunction.onCallBack(Response.success(request.id).toJson())
                        close()
                    }

                    else -> {
                        callbackFunction.onCallBack(
                            Response.error(request.id, -1, "not support").toJson()
                        )
                    }
                }
            }
        }
    }

    private suspend fun withdrawPoolEarnings(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        // TODO
        val accountManager = WalletAppViewModel.getViewModelInstance().mAccountManager
        val violasAccount = withContext(Dispatchers.IO) {
            accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
        }
        if (violasAccount == null) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "Account does not exist").toJson()
            )
            return
        }

        authenticateAccount(
            violasAccount,
            accountManager,
            cancelCallback = {
                callbackFunction.onCallBack(
                    Response.error(request.id, -1, "User canceled").toJson()
                )
            }
        ) {
            launch {

            }
        }
    }

    private suspend fun withdrawBankEarnings(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        // TODO
        val accountManager = WalletAppViewModel.getViewModelInstance().mAccountManager
        val violasAccount = withContext(Dispatchers.IO) {
            accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
        }
        if (violasAccount == null) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "Account does not exist").toJson()
            )
            return
        }

        authenticateAccount(
            violasAccount,
            accountManager,
            cancelCallback = {
                callbackFunction.onCallBack(
                    Response.error(request.id, -1, "User canceled").toJson()
                )
            }
        ) {
            launch {

            }
        }
    }

    private fun gotoHomePoolPage(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Market))
        EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.Pool))
        EventBus.getDefault().post(SwitchMarketPoolOpModeEvent(MarketPoolOpMode.TransferIn))
        callbackFunction.onCallBack(Response.success(request.id).toJson())
        close()
    }

    private fun gotoHomeBankPage(
        request: Request,
        callbackFunction: CallBackFunction,
        deposit: Boolean
    ) {
        EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Market))
        EventBus.getDefault()
            .post(SwitchBankPageEvent(if (deposit) BankPageType.Deposit else BankPageType.Borrowing))
        callbackFunction.onCallBack(Response.success(request.id).toJson())
        close()
    }

    private fun startEarningsDetailsPage(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        callbackFunction.onCallBack(Response.success(request.id).toJson())
        IncentiveEarningsDetailsActivity.start(this)
    }

    private suspend fun startReceiveRewardsPage(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        val accountManager = WalletAppViewModel.getViewModelInstance().mAccountManager
        val violasAccount = withContext(Dispatchers.IO) {
            accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
        }
        if (violasAccount == null) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "Account does not exist").toJson()
            )
            return
        }

        callbackFunction.onCallBack(Response.success(request.id).toJson())
        ReceiveIncentiveRewardsActivity.start(this)
    }

    class Response<T>(
        val id: String,
        val result: T? = null,
        val error: Error? = null
    ) {

        class Error(
            val code: Int = 1,
            val message: String = ""
        )

        companion object {
            fun error(id: String, code: Int, message: String): Response<Any> {
                return Response<Any>(id, null, Error(code, message))
            }

            fun success(id: String): Response<String> {
                return Response(id, "success")
            }
        }
    }

    data class Request(
        val method: String,
        val params: Array<String>,
        val id: String = Random.nextInt().toString()
    )
}
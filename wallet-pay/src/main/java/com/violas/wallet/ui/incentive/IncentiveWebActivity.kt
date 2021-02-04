package com.violas.wallet.ui.incentive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.Keep
import com.github.lzyzsd.jsbridge.CallBackFunction
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.logInfo
import com.palliums.utils.saveIntoSystemAlbum
import com.palliums.utils.start
import com.palliums.utils.toBitmap
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseBridgeWebActivity
import com.violas.wallet.biz.ExchangeManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.EXTRA_KEY_TITLE
import com.violas.wallet.common.EXTRA_KEY_URL
import com.violas.wallet.common.SYSTEM_ALBUM_DIR_NAME
import com.violas.wallet.event.*
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.ui.incentive.earningsDetails.IncentiveEarningsDetailsActivity
import com.violas.wallet.ui.incentive.receiveRewards.ReceiveIncentiveRewardsActivity
import com.violas.wallet.ui.main.market.pool.MarketPoolOpMode
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.activity_bridge_web.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import kotlin.random.Random

/**
 * Created by elephant on 12/15/20 4:19 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 挖矿激励web页面
 */
class IncentiveWebActivity : BaseBridgeWebActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val TAG = "IncentiveWebActivity"
        private const val REQUEST_CODE_SAVE_PICTURE = 100

        @JvmStatic
        private fun start(context: Context, url: String, title: String? = null) {
            Intent(context, IncentiveWebActivity::class.java).also {
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
            val url = "https://wallet.violas.io/homepage/home/miningAwards?language=${
                getLanguageCode()
            }&address=${getViolasAddress() ?: ""}"
            start(context, url)
        }

        /**
         * 打开邀请好友首页
         */
        suspend fun startInviteHomePage(context: Context) {
            val url = "https://wallet.violas.io/homepage/home/inviteRewards?language=${
                getLanguageCode()
            }&address=${getViolasAddress() ?: ""}"
            start(context, url)
        }

        /**
         * 打开激励挖矿规则页面
         */
        suspend fun startIncentiveRules(context: Context) {
            val url = "https://wallet.violas.io/homepage/home/ruleDescription?language=${
                getLanguageCode()
            }&address=${getViolasAddress() ?: ""}"
            start(context, url)
        }
    }

    private val mTitle by lazy { intent.getStringExtra(EXTRA_KEY_TITLE) }
    private val mUrl by lazy { intent.getStringExtra(EXTRA_KEY_URL) }
    private val mGson by lazy { Gson() }
    private val mBankManager by lazy { BankManager() }
    private val mExchangeManager by lazy { ExchangeManager() }

    private var savePictureRequest: Request? = null
    private var savePictureCallbackFunction: CallBackFunction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        EventBus.getDefault().register(this)
        super.onCreate(savedInstanceState)
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
                        withdrawMiningRewards(request, callbackFunction, false)
                    }

                    "withdraw_bank_profit" -> {
                        withdrawMiningRewards(request, callbackFunction, true)
                    }

                    "new_user_check" -> {
                        startReceiveRewardsPage(request, callbackFunction)
                    }

                    "pool_farming" -> {
                        backToHomePoolPage(request, callbackFunction)
                    }

                    "bank_deposit_farming" -> {
                        backToHomeBankPage(request, callbackFunction, true)
                    }

                    "bank_loan_farming" -> {
                        backToHomeBankPage(request, callbackFunction, false)
                    }

                    "yield_farming_detail",
                    "mine_invite" -> {
                        startEarningsDetailsPage(request, callbackFunction)
                    }

                    "save_picture" -> {
                        handleSavePictureRequest(request, callbackFunction)
                    }

                    "share_link" -> {
                        shareLink(request, callbackFunction)
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

    private suspend fun withdrawMiningRewards(
        request: Request,
        callbackFunction: CallBackFunction,
        bankMining: Boolean
    ) {
        val accountManager = WalletAppViewModel.getViewModelInstance().mAccountManager
        val violasAccount = withContext(Dispatchers.IO) {
            accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
        }
        if (violasAccount == null) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "Account does not exist").toJson()
            )
            showToast(R.string.common_tips_account_empty)
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
            sendWithdrawMiningRewardsTxn(request, callbackFunction, bankMining, it)
        }
    }

    private fun sendWithdrawMiningRewardsTxn(
        request: Request,
        callbackFunction: CallBackFunction,
        bankMining: Boolean,
        privateKey: ByteArray
    ) {
        launch {
            val exception = withContext(Dispatchers.IO) {
                return@withContext try {
                    if (bankMining) {
                        mBankManager.withdrawReward(privateKey)
                    } else {
                        mExchangeManager.withdrawReward(privateKey)
                    }
                    null
                } catch (e: Exception) {
                    e
                }
            }

            delay(500)
            dismissProgress()

            if (exception != null) {
                callbackFunction.onCallBack(
                    Response.error(
                        request.id,
                        -2,
                        exception.message ?: "unknown error"
                    ).toJson()
                )
                showToast(exception.getShowErrorMessage(R.string.incentive_tips_active_extraction_failure))
                return@launch
            }

            CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
            callbackFunction.onCallBack(Response.success(request.id).toJson())
            startLoad()
            showToast(R.string.incentive_tips_active_extraction_success)
        }
    }

    private fun backToHomePoolPage(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Market))
        EventBus.getDefault().post(SwitchMarketPageEvent(MarketPageType.Pool))
        EventBus.getDefault().post(SwitchMarketPoolOpModeEvent(MarketPoolOpMode.TransferIn))
        callbackFunction.onCallBack(Response.success(request.id).toJson())
        close()
    }

    private fun backToHomeBankPage(
        request: Request,
        callbackFunction: CallBackFunction,
        deposit: Boolean
    ) {
        EventBus.getDefault().post(SwitchHomePageEvent(HomePageType.Bank))
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
            showToast(R.string.common_tips_account_empty)
            return
        }

        callbackFunction.onCallBack(Response.success(request.id).toJson())
        ReceiveIncentiveRewardsActivity.start(this)
    }

    private suspend fun handleSavePictureRequest(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        if (request.params.size != 1) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "param is error").toJson()
            )
            return
        }

        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            savePicture(request, callbackFunction)
        } else {
            savePictureRequest = request
            savePictureCallbackFunction = callbackFunction
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_CODE_SAVE_PICTURE, *perms)
                    .setRationale(R.string.save_picture_need_permissions_desc)
                    .setNegativeButtonText(R.string.common_action_cancel)
                    .setPositiveButtonText(R.string.common_action_ok)
                    .setTheme(R.style.AppAlertDialog)
                    .build()
            )
        }
    }

    private fun shareLink(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        if (request.params.size != 1) {
            callbackFunction.onCallBack(
                Response.error(request.id, -1, "param is error").toJson()
            )
            return
        }

        val url = request.params[0]
        ClipboardUtils.copy(this, url, R.string.incentive_tips_share_link_success)
        callbackFunction.onCallBack(Response.success(request.id).toJson())
    }

    @Keep
    class Response<T>(
        val id: String,
        val result: T? = null,
        val error: Error? = null
    ) {

        @Keep
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

    @Keep
    data class Request(
        val method: String,
        val params: Array<String>,
        val id: String = Random.nextInt().toString()
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            REQUEST_CODE_SAVE_PICTURE -> {
                launch {
                    if (savePictureRequest != null && savePictureCallbackFunction != null) {
                        savePictureCallbackFunction!!.onCallBack(
                            Response.error(
                                savePictureRequest!!.id,
                                -2,
                                "No storage permission"
                            ).toJson()
                        )
                    }
                    savePictureRequest = null
                    savePictureCallbackFunction = null
                }
            }
        }

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setTitle(getString(R.string.save_picture_set_permissions_title))
                .setRationale(getString(R.string.save_picture_set_permissions_desc))
                .setNegativeButton(R.string.common_action_cancel)
                .setPositiveButton(R.string.common_action_ok)
                .setThemeResId(R.style.AppAlertDialog)
                .build()
                .show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            REQUEST_CODE_SAVE_PICTURE -> {
                launch {
                    if (savePictureRequest != null && savePictureCallbackFunction != null) {
                        savePicture(
                            savePictureRequest!!,
                            savePictureCallbackFunction!!
                        )
                    }
                    savePictureRequest = null
                    savePictureCallbackFunction = null
                }
            }
        }
    }

    private suspend fun savePicture(
        request: Request,
        callbackFunction: CallBackFunction
    ) {
        showProgress()

        // 针对某些设备首次申请权限后立即保存失败的问题
        delay(500)

        // base64字符转Bitmap
        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            val base64Str = request.params[0]
            base64Str.toBitmap()
        }

        if (bitmap == null) {
            dismissProgress()
            showToast(R.string.save_picture_tips_failure)
            callbackFunction.onCallBack(
                Response.error(request.id, -2, "Failed to convert to image").toJson()
            )
            return
        }

        // 保存图片到系统相册
        val saveResult: Boolean = withContext(Dispatchers.IO) {
            bitmap.saveIntoSystemAlbum(SYSTEM_ALBUM_DIR_NAME)
        }

        dismissProgress()
        if (saveResult) {
            showToast(R.string.save_picture_tips_success)
            callbackFunction.onCallBack(
                Response.success(request.id).toJson()
            )
        } else {
            showToast(R.string.save_picture_tips_failure)
            callbackFunction.onCallBack(
                Response.error(request.id, -2, "Failed to save to system album").toJson()
            )
        }
    }

}
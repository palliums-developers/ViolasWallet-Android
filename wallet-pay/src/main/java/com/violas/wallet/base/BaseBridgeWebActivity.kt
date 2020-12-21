package com.violas.wallet.base

import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.github.lzyzsd.jsbridge.BridgeWebViewClient
import com.violas.wallet.R
import kotlinx.android.synthetic.main.activity_bridge_web.*


/**
 * Created by elephant on 2019-10-31 17:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Web页面基类
 */
abstract class BaseBridgeWebActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_bridge_web
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = getFixedTitle()
        title?.let { setTitle(it) }

        vFailed.visibility = View.GONE
        vFailed.setOnClickListener(this)

        initWebView()
        startLoad()
    }

    override fun onTitleLeftViewClick() {
        close()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        stopLoad()
        startLoad()
    }

    override fun onResume() {
        vWeb.onResume()
        super.onResume()
    }

    override fun onPause() {
        vWeb.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopLoad()
        try {
            vWeb.loadDataWithBaseURL(
                null, "", "text/html", "utf-8", null
            )
            vWeb.clearHistory()

            (vWeb.parent as ViewGroup).removeView(vWeb)
            vWeb.destroy()
        } catch (e: Exception) {
            // ignore
        }

        super.onDestroy()
    }

    override fun onBackPressedSupport() {
        if (vWeb.canGoBack()) {
            vWeb.goBack()
        } else {
            super.onBackPressedSupport()
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vFailed -> {
                vWeb.reload()
            }
        }
    }

    /**
     * 获取Web页面加载的url
     */
    protected abstract fun getUrl(): String

    /**
     * 获取Web页面固定的标题，若返回null，则使用从web中解析的title
     */
    protected abstract fun getFixedTitle(): String?

    protected fun startLoad() {
        vProgress.progress = 0
        vWeb.visibility = View.VISIBLE
        vWeb.loadUrl(getUrl())
    }

    protected fun stopLoad() {
        try {
            vWeb.stopLoading()
        } catch (e: Exception) {
            // ignore
        }
    }

    protected open fun initWebView() {
        val settings = vWeb.settings

        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.domStorageEnabled = true

        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.allowContentAccess = true

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        /*settings.defaultFontSize = 30
        settings.minimumFontSize = 12*/

        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

        // 5.0以上开启混合模式加载(https加载时需要)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        vWeb.overScrollMode = View.OVER_SCROLL_NEVER
        vWeb.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                vProgress?.let {
                    it.progress = when {
                        newProgress > 100 -> 100
                        newProgress < 0 -> 0
                        else -> newProgress
                    }
                }
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                showToast("onJsAlert: $message")
                val b: AlertDialog.Builder = AlertDialog.Builder(this@BaseBridgeWebActivity)
                b.setTitle("Alert")
                b.setMessage(message)
                b.setPositiveButton(android.R.string.ok) { dialog, which ->
                    result!!.confirm()
                }
                b.setCancelable(false)
                b.create().show()
                return super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                showToast("onJsConfirm: $message")
                return super.onJsConfirm(view, url, message, result)
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                showToast("onJsPrompt: $message")
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onJsBeforeUnload(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                showToast("onJsBeforeUnload: $message")
                return super.onJsBeforeUnload(view, url, message, result)
            }

        }
        vWeb.webViewClient = object : BridgeWebViewClient(vWeb) {

            /*override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }*/

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                vFailed?.visibility = View.GONE
                vProgress?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                vProgress?.visibility = View.GONE

                if (getFixedTitle() == null) {
                    val title = view?.title
                    title?.let { setTitle(title) }
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)
                handler?.proceed()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    handleError()
                }
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    handleError()
                }
            }

            private fun handleError() {
                vFailed?.visibility = View.VISIBLE
                vWeb?.visibility = View.GONE
            }
        }
    }
}
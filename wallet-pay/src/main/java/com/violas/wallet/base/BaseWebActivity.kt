package com.violas.wallet.base

import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import com.violas.wallet.R
import kotlinx.android.synthetic.main.activity_base_web.*

/**
 * Created by elephant on 2019-10-31 17:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Web页面基类
 */
abstract class BaseWebActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_base_web
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = getFixedTitle()
        title?.let { setTitle(it) }

        getFailedView()?.visibility = View.GONE
        getFailedView()?.setOnClickListener(this)

        getWebView()?.let { initWebView(it) }
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
        getWebView()?.onResume()
        super.onResume()
    }

    override fun onPause() {
        getWebView()?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopLoad()
        try {
            getWebView()?.let {
                it.loadDataWithBaseURL(
                    null, "", "text/html", "utf-8", null
                )
                it.clearHistory()

                (it.parent as ViewGroup).removeView(it)
                it.destroy()
            }
        } catch (e: Exception) {
            // ignore
        }

        super.onDestroy()
    }

    override fun onBackPressedSupport() {
        if (getWebView()?.canGoBack() == true) {
            getWebView()?.goBack()
        } else {
            super.onBackPressedSupport()
        }
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.vFailed -> {
                getWebView()?.reload()
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

    open fun getWebView(): WebView? {
        return vWeb
    }

    open fun getProgressView(): ProgressBar? {
        return vProgress
    }

    open fun getFailedView(): View? {
        return vFailed
    }

    open fun startLoad() {
        getProgressView()?.progress = 0
        getWebView()?.visibility = View.VISIBLE
        getWebView()?.loadUrl(getUrl())
    }

    open fun onPageStarted() {
        getFailedView()?.visibility = View.GONE
        getProgressView()?.visibility = View.VISIBLE
    }

    open fun onLoadError() {
        getFailedView()?.visibility = View.VISIBLE
        getWebView()?.visibility = View.GONE
    }

    private fun stopLoad() {
        try {
            getWebView()?.stopLoading()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun initWebView(webView: WebView) {
        val settings = webView.settings

        settings.javaScriptEnabled = true
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

        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                getProgressView()?.let {
                    it.progress = when {
                        newProgress > 100 -> 100
                        newProgress < 0 -> 0
                        else -> newProgress
                    }
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {

            /*override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }*/

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageStarted()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                getProgressView()?.visibility = View.GONE

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
                    onLoadError()
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
                    onLoadError()
                }
            }
        }
    }
}
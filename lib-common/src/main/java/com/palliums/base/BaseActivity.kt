package com.palliums.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.palliums.R
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.loading.LoadingDialog
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.layout_status_bar.*
import kotlinx.android.synthetic.main.layout_title_bar.*
import kotlinx.coroutines.*
import me.yokeyword.fragmentation.SupportActivity
import qiu.niorgai.StatusBarCompat

abstract class BaseActivity : SupportActivity(), View.OnClickListener, ViewController,
    CoroutineScope by MainScope() {

    private var mLoadingDialog: LoadingDialog? = null

    abstract fun getLayoutResId(): Int
    protected open fun getLayoutView(): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //透明状态栏，布局延伸到状态栏中
        StatusBarCompat.translucentStatusBar(this, true)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_base)

        val layoutView =
            getLayoutView() ?: layoutInflater.inflate(getLayoutResId(), vRootView, false)
        vRootView.addView(layoutView)

        vStatusBar.layoutParams.height = StatusBarUtil.getStatusBarHeight(this)

        vTitleLeftImageBtn?.setOnClickListener(this)
    }

    override fun onDestroy() {
        try {
            mLoadingDialog?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cancel()
        super.onDestroy()
    }

    /**
     * 设置标题
     */
    override fun setTitle(@StringRes strId: Int) {
        if (vTitleMiddleText != null && strId != 0) {
            vTitleMiddleText.setText(strId)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (vTitleMiddleText != null && !title.isNullOrEmpty()) {
            vTitleMiddleText.text = title
        }
    }

    fun setTitleRightText(@StringRes strId: Int) {
        if (vTitleRightTextBtn != null && strId != 0) {
            vTitleRightTextBtn.setText(strId)
            vTitleRightTextBtn.visibility = View.VISIBLE
            vTitleRightTextBtn.setOnClickListener(this)
        } else if (vTitleRightTextBtn != null) {
            vTitleRightTextBtn.visibility = View.GONE
        }
    }

    fun setTitleRightImage(@DrawableRes resId: Int) {
        if (vTitleRightImageBtn != null && resId != 0) {
            vTitleRightImageBtn.setImageResource(resId)
            vTitleRightImageBtn.visibility = View.VISIBLE
            vTitleRightImageBtn.setOnClickListener(this)
        } else if (vTitleRightImageBtn != null) {
            vTitleRightImageBtn.visibility = View.GONE
        }
    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            // TitleBar的View点击事件与页面其它的View点击事件分开处理
            when (view.id) {
                R.id.vTitleLeftImageBtn ->
                    onTitleLeftViewClick()

                R.id.vTitleRightTextBtn,
                R.id.vTitleRightImageBtn ->
                    onTitleRightViewClick()

                else ->
                    onViewClick(view)
            }
        }
    }

    /**
     * TitleBar的左侧View点击回调，已防重点击处理
     * 默认关闭当前页面
     */
    protected open fun onTitleLeftViewClick() {
        finish()
    }

    /**
     * TitleBar的右侧View点击回调，已防重点击处理
     * 没有响应逻辑
     */
    protected open fun onTitleRightViewClick() {

    }

    /**
     * View点击回调，已防重点击处理
     * 该回调只会分发页面非TitleBar的View点击事件
     * 如需处理TitleBar的View点击事件，请按需覆写[onTitleLeftViewClick]和[onTitleRightViewClick]
     * @param view
     */
    protected open fun onViewClick(view: View) {

    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        try {
            launch {
                mLoadingDialog?.dismiss()
                mLoadingDialog = LoadingDialog()
                    .setMessage(msg)
                mLoadingDialog?.show(supportFragmentManager, "load")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun dismissProgress() {
        try {
            launch {
                mLoadingDialog?.dismiss()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    override fun showToast(@StringRes msgId: Int) {
        showToast(getString(msgId))
    }

    override fun showToast(msg: String) {
        launch {
            Toast.makeText(this@BaseActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val handler = CoroutineExceptionHandler { _, exception ->
        System.err.println("Caught $exception")
    }
}

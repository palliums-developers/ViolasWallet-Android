package com.palliums.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.loading.LoadingDialog
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.layout_title_bar.*
import kotlinx.coroutines.*
import me.yokeyword.fragmentation.SupportActivity
import qiu.niorgai.StatusBarCompat

abstract class BaseActivity : SupportActivity(), View.OnClickListener, ViewController,
    CoroutineScope by MainScope() {

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //透明状态栏，布局延伸到状态栏中
        StatusBarCompat.translucentStatusBar(this, true)
        super.onCreate(savedInstanceState)

        setContentView(com.palliums.R.layout.activity_base)

        val layoutView =
            getLayoutView() ?: layoutInflater.inflate(getLayoutResId(), vContentContainer, false)
        vContentContainer.addView(layoutView)

        val statusBarHeight = StatusBarUtil.getStatusBarHeight(this)
        vTitleBar.setPadding(0, statusBarHeight, 0, 0)
        vTitleBar.layoutParams.height = DensityUtility.dp2px(this, 48) + statusBarHeight
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

    abstract fun getLayoutResId(): Int

    protected open fun getLayoutView(): View? = null

    /**
     * 设置标题
     */
    override fun setTitle(@StringRes strId: Int) {
        if (strId != 0) {
            vTitleMiddleText.setText(strId)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (!title.isNullOrEmpty()) {
            vTitleMiddleText.text = title
        }
    }

    override fun setTitleColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleMiddleText.setTextColor(com.palliums.utils.getColor(resId, this))
        }
    }

    fun setTitleLeftImageResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleLeftImageBtn.setImageResource(resId)
            vTitleLeftImageBtn.visibility = View.VISIBLE
            vTitleLeftImageBtn.setOnClickListener(this)
        } else {
            vTitleLeftImageBtn.visibility = View.GONE
        }
    }

    fun setTitleRightText(@StringRes resId: Int) {
        if (resId != 0) {
            vTitleRightTextBtn.setText(resId)
            vTitleRightTextBtn.visibility = View.VISIBLE
            vTitleRightTextBtn.setOnClickListener(this)
        } else {
            vTitleRightTextBtn.visibility = View.GONE
        }
    }

    fun setTitleRightTextColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleRightTextBtn.setTextColor(com.palliums.utils.getColor(resId, this))
        }
    }

    fun setTitleRightImageResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleRightImageBtn.setImageResource(resId)
            vTitleRightImageBtn.visibility = View.VISIBLE
            vTitleRightImageBtn.setOnClickListener(this)
        } else {
            vTitleRightImageBtn.visibility = View.GONE
        }
    }

    fun setTitleBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleBarBackground.setBackgroundResource(resId)
        }
    }

    fun setTitleBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleBar.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    fun setContentBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vContentContainer.setBackgroundResource(resId)
        }
    }

    fun setContentBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vContentContainer.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    fun setRootBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vRootView.setBackgroundResource(resId)
        }
    }

    fun setRootBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vRootView.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    fun setTitleBarVisibility(visibility: Int) {
        vTitleBar.visibility = visibility
    }

    fun setTitleLeftViewVisibility(visibility: Int) {
        vTitleLeftImageBtn.visibility = visibility
    }

    /**
     * 防重点击处理，子类复写[onViewClick]来响应事件
     */
    final override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            // TitleBar的View点击事件与页面其它的View点击事件分开处理
            when (view.id) {
                com.palliums.R.id.vTitleLeftImageBtn ->
                    onTitleLeftViewClick()

                com.palliums.R.id.vTitleRightTextBtn,
                com.palliums.R.id.vTitleRightImageBtn ->
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

    /**
     * 浅色状态模式，设置字体为深色
     */
    protected fun setLightStatusBar(isLightStatusBar: Boolean) {
        StatusBarUtil.setLightStatusBarMode(this, isLightStatusBar)
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

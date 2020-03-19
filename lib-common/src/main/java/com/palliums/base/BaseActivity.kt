package com.palliums.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import com.palliums.utils.CustomMainScope
import com.palliums.utils.DensityUtility
import com.palliums.utils.StatusBarUtil
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.loading.LoadingDialog
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.layout_title_bar.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.yokeyword.fragmentation.SupportActivity
import qiu.niorgai.StatusBarCompat

abstract class BaseActivity : SupportActivity(), View.OnClickListener, ViewController,
    CoroutineScope by CustomMainScope() {

    private var mLoadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //透明状态栏，布局延伸到状态栏中
        StatusBarCompat.translucentStatusBar(
            this,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        )
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

    fun close() {
        if (!isDestroyed && !isFinishing) {
            ActivityCompat.finishAfterTransition(this)
        }
    }

    abstract fun getLayoutResId(): Int

    protected open fun getLayoutView(): View? = null

    private fun adjustTitlePaddingHorizontal() {
        vTitleMiddleText.post {
            val paddingHorizontal = vTitleLeftImageBtn.width.coerceAtLeast(
                vTitleRightImageBtn.width.coerceAtLeast(vTitleRightTextBtn.width)
            )
            vTitleMiddleText.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
        }
    }

    /**
     * 设置标题
     */
    override fun setTitle(@StringRes strId: Int) {
        if (strId != 0) {
            vTitleMiddleText.setText(strId)
            adjustTitlePaddingHorizontal()
        }
    }

    /**
     * 设置标题
     */
    override fun setTitle(title: CharSequence?) {
        if (!title.isNullOrEmpty()) {
            vTitleMiddleText.text = title
            adjustTitlePaddingHorizontal()
        }
    }

    /**
     * 设置标题的字体颜色
     */
    override fun setTitleColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleMiddleText.setTextColor(com.palliums.utils.getColor(resId, this))
        }
    }

    /**
     * 设置标题栏左侧按钮的图片资源
     */
    fun setTitleLeftImageResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleLeftImageBtn.setImageResource(resId)
            vTitleLeftImageBtn.visibility = View.VISIBLE
            vTitleLeftImageBtn.setOnClickListener(this)
        } else {
            vTitleLeftImageBtn.visibility = View.GONE
        }
        adjustTitlePaddingHorizontal()
    }

    /**
     * 设置标题栏右侧按钮的文字
     */
    fun setTitleRightText(@StringRes resId: Int) {
        if (resId != 0) {
            vTitleRightTextBtn.setText(resId)
            vTitleRightTextBtn.visibility = View.VISIBLE
            vTitleRightTextBtn.setOnClickListener(this)

            vTitleRightImageBtn.visibility = View.GONE
        } else {
            vTitleRightTextBtn.visibility = View.GONE
        }
        adjustTitlePaddingHorizontal()
    }

    /**
     * 设置标题栏右侧按钮的字体颜色
     */
    fun setTitleRightTextColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleRightTextBtn.setTextColor(com.palliums.utils.getColor(resId, this))
        }
    }

    /**
     * 设置标题栏右侧按钮的图片资源
     */
    fun setTitleRightImageResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleRightImageBtn.setImageResource(resId)
            vTitleRightImageBtn.visibility = View.VISIBLE
            vTitleRightImageBtn.setOnClickListener(this)

            vTitleRightTextBtn.visibility = View.GONE
        } else {
            vTitleRightImageBtn.visibility = View.GONE
        }
        adjustTitlePaddingHorizontal()
    }

    /**
     * 设置标题栏的背景资源
     */
    fun setTitleBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTitleBar.setBackgroundResource(resId)
        }
    }

    /**
     * 设置标题栏的背景色
     */
    fun setTitleBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vTitleBar.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    /**
     * 设置内容视图的背景资源，该内容视图不包含标题栏
     */
    fun setContentBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vContentContainer.setBackgroundResource(resId)
        }
    }

    /**
     * 设置内容视图的背景色，该内容视图不包含标题栏
     */
    fun setContentBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vContentContainer.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    /**
     * 设置顶部视图的背景资源，顶部视图在标题栏之下，在根视图之上
     */
    fun setTopBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vTopView.setBackgroundResource(resId)
        }
    }

    /**
     * 设置根视图的背景资源
     */
    fun setRootBackgroundResource(@DrawableRes resId: Int) {
        if (resId != 0) {
            vRootView.setBackgroundResource(resId)
        }
    }

    /**
     * 设置根视图的背景色
     */
    fun setRootBackgroundColor(@ColorRes resId: Int) {
        if (resId != 0) {
            vRootView.setBackgroundColor(com.palliums.utils.getColor(resId, this))
        }
    }

    /**
     * 设置标题栏的可见性状态
     */
    fun setTitleBarVisibility(visibility: Int) {
        vTitleBar.visibility = visibility
    }

    /**
     * 设置标题栏左侧按钮的可见性状态
     */
    fun setTitleLeftViewVisibility(visibility: Int) {
        vTitleLeftImageBtn.visibility = visibility
        adjustTitlePaddingHorizontal()
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
        onBackPressedSupport()
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

    fun setStatusBarMode(darkMode: Boolean): Int {
        var result = 0
        try {
            if (setMIUIStatusBarMode(darkMode)) {
                result = 1
            } else if (setFlymeStatusBarMode(darkMode)) {
                result = 2
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = if (darkMode)
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                else
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun setMIUIStatusBarMode(darkMode: Boolean): Boolean {
        var result = false
        val window = window
        if (window != null) {
            val clazz = window.javaClass
            try {
                var darkModeFlag = 0
                val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                darkModeFlag = field.getInt(layoutParams)
                val extraFlagField = clazz.getMethod(
                    "setExtraFlags",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                if (darkMode) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag)//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag)//清除黑色字体
                }
                result = true
            } catch (e: Exception) {

            }

        }
        return result
    }

    private fun setFlymeStatusBarMode(darkMode: Boolean): Boolean {
        var result = false
        val window = window
        if (window != null) {
            try {
                val lp = window.attributes
                val darkFlag = WindowManager.LayoutParams::class.java
                    .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
                val meizuFlags = WindowManager.LayoutParams::class.java
                    .getDeclaredField("meizuFlags")
                darkFlag.isAccessible = true
                meizuFlags.isAccessible = true
                val bit = darkFlag.getInt(null)
                var value = meizuFlags.getInt(lp)
                value = if (darkMode) {
                    value or bit
                } else {
                    value and bit.inv()
                }
                meizuFlags.setInt(lp, value)
                window.attributes = lp
                result = true
            } catch (e: Exception) {

            }

        }
        return result
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        try {
            launch {
                if(mLoadingDialog == null){
                    mLoadingDialog = LoadingDialog()
                        .setMessage(msg)
                    mLoadingDialog!!.show(supportFragmentManager, "load")
                }else{
                    mLoadingDialog!!.setMessage(msg)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun dismissProgress() {
        try {
            launch {
                mLoadingDialog?.dismissAllowingStateLoss()
                mLoadingDialog = null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        launch {
            Toast.makeText(this@BaseActivity, msg, duration).show()
        }
    }

    val handler = CoroutineExceptionHandler { _, exception ->
        System.err.println("Caught $exception")
    }
}

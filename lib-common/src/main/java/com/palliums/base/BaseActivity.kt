package com.palliums.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.palliums.widget.loading.LoadingDialog
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

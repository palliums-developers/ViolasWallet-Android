package com.violas.wallet.ui.main.me

import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.palliums.base.BaseDialogFragment
import com.palliums.utils.CustomMainScope
import com.violas.wallet.R
import kotlinx.android.synthetic.main.dialog_apply_for_licence.*
import kotlinx.coroutines.CoroutineScope

/**
 * Created by elephant on 2020/3/9 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ApplyForLicenceDialog : BaseDialogFragment(), CoroutineScope by CustomMainScope() {

    private var mApplyForCallback: ((BaseDialogFragment) -> Unit)? = null

    override fun getLayoutResId(): Int {
        return R.layout.dialog_apply_for_licence
    }

    override fun getWindowAnimationsStyleId(): Int {
        return R.style.AnimationDefaultFadeDialog
    }

    override fun getWindowLayoutParamsGravity(): Int {
        return Gravity.CENTER or Gravity.TOP
    }

    override fun canceledOnTouchOutside(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnApplyFor.setOnClickListener {
            mApplyForCallback?.invoke(this)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            val attributes = it.attributes
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)
            attributes.y = (point.y * 0.2).toInt()
            it.attributes = attributes
        }
    }

    fun setApplyForCallback(callback: (BaseDialogFragment) -> Unit): ApplyForLicenceDialog {
        mApplyForCallback = callback
        return this
    }
}
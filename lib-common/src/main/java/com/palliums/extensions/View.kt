package com.palliums.extensions

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import com.palliums.content.ContextProvider
import com.palliums.utils.DensityUtility

fun View.expandTouchArea(dp: Int = 10) {
    val size = DensityUtility.dp2px(ContextProvider.getContext(), dp)
    val parentView: View = this.parent as View
    parentView.post {
        val rect = Rect()
        this.getHitRect(rect)
        rect.top -= size
        rect.bottom += size
        rect.left -= size
        rect.right += size
        parentView.touchDelegate = TouchDelegate(rect, this)
    }
}
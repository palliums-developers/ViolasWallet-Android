package com.violas.wallet.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.violas.wallet.R
import kotlinx.android.synthetic.main.view_upload_image.view.*

class UploadImageView : LinearLayout {
    private var view: View? = null
    private var attrs: AttributeSet? = null
    private var defStyleAttr: Int = 0
    private var defStyleRes: Int = 0
    private var closeCallBack: (() -> Unit)? = null

    constructor(
        context: Context
    ) : super(context) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        this.attrs = attrs
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.attrs = attrs
        this.defStyleAttr = defStyleAttr
        this.defStyleRes = defStyleRes
        init()
    }

    fun init() {
        loadDefValue()
        view = LayoutInflater.from(context).inflate(R.layout.view_upload_image, this, false)
        addView(view)

        view?.ivClose?.setOnClickListener {
            closeContentImage()
        }
    }

    private fun loadDefValue() {

    }

    fun setCloseContentCallback(call: () -> Unit) {
        closeCallBack = call
    }

    fun setContentImage(drawable: Drawable?) {
        drawable?.let {
            view?.ivContent?.background = drawable
            view?.ivClose?.visibility = View.VISIBLE
            view?.ivAdd?.visibility = View.GONE
        }
    }

    fun closeContentImage() {
        view?.ivContent?.setBackgroundColor(Color.parseColor("#f8f9fa"))
        view?.ivClose?.visibility = View.GONE
        view?.ivAdd?.visibility = View.VISIBLE
        view?.progress?.visibility = View.GONE
        closeCallBack?.invoke()
    }

    fun startLoadingImage() {
        view?.progress?.visibility = View.VISIBLE
        view?.ivAdd?.visibility = View.GONE
    }

    fun endLoadingImage() {
        view?.progress?.visibility = View.GONE
    }
}
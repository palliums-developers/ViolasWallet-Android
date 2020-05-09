package com.violas.wallet.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.bumptech.glide.request.target.Target
import com.violas.wallet.R
import com.violas.wallet.image.GlideApp
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

    fun getContentImageView() = view?.ivContent

    fun setCloseContentCallback(call: () -> Unit) {
        closeCallBack = call
    }

    fun setContentImage(imageUrl: String, onlyShow: Boolean = false) {
        view?.ivClose?.visibility = if (onlyShow) View.GONE else View.VISIBLE
        view?.ivAdd?.visibility = View.GONE
        view?.tvDesc?.visibility = View.GONE
        view?.ivContent?.background = null
        view?.ivContent?.let {
            GlideApp.with(this)
                .load(imageUrl)
                .centerCrop()
                .override(Target.SIZE_ORIGINAL)
                .placeholder(R.drawable.shape_bg_photo)
                .error(R.drawable.shape_bg_photo)
                .into(it)
        }
    }

    fun setContentImage(drawable: Drawable?) {
        drawable?.let {
            view?.ivContent?.setImageDrawable(drawable)
            view?.ivClose?.visibility = View.VISIBLE
            view?.ivAdd?.visibility = View.GONE
            view?.tvDesc?.visibility = View.GONE
        }
    }

    fun closeContentImage() {
        view?.ivClose?.visibility = View.GONE
        view?.ivAdd?.visibility = View.VISIBLE
        view?.tvDesc?.visibility = View.VISIBLE
        view?.progress?.visibility = View.GONE
        closeCallBack?.invoke()
    }

    fun startLoadingImage() {
        view?.progress?.visibility = View.VISIBLE
        view?.ivAdd?.visibility = View.GONE
        view?.tvDesc?.visibility = View.GONE
    }

    fun endLoadingImage() {
        view?.progress?.visibility = View.GONE
    }
}
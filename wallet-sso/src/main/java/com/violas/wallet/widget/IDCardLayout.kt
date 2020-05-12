package com.violas.wallet.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.palliums.utils.isFastMultiClick
import com.violas.wallet.R
import com.violas.wallet.image.GlideApp
import kotlinx.android.synthetic.main.widget_id_card_layout.view.*

/**
 * Created by elephant on 2019-11-28 18:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份证视图
 */
class IDCardLayout : FrameLayout, View.OnClickListener {

    private var idCardDefault: Drawable? = null
    private var photographDesc: String? = null

    private var onViewClickListener: OnViewClickListener? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, 0, 0)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        View.inflate(context, R.layout.widget_id_card_layout, this)

        attrs?.let { it ->
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.IDCardLayout,
                defStyleAttr,
                defStyleRes
            )

            idCardDefault = typedArray.getDrawable(R.styleable.IDCardLayout_icl_id_card_default)
            photographDesc = typedArray.getString(R.styleable.IDCardLayout_icl_photograph_desc)
        }

        idCardDefault?.let { setIDCardDefault(it) }
        photographDesc?.let { setPhotographDesc(it) }

        ivDelete.setOnClickListener(this)
        ivDeleteBg.setOnClickListener(this)

        changeViewVisibility(false)
    }

    override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            when (view.id) {
                R.id.ivDelete,
                R.id.ivDeleteBg -> {
                    onViewClickListener?.onClickDelete()
                }
            }
        }
    }

    fun getIDCardImageView() = ivIdCard

    fun setIDCardDefault(@DrawableRes resId: Int) {
        ivIdCard.setImageResource(resId)
    }

    fun setIDCardDefault(drawable: Drawable) {
        ivIdCard.setImageDrawable(drawable)
    }

    fun setPhotographDesc(@StringRes resId: Int) {
        tvPhotographDesc.setText(resId)
    }

    fun setPhotographDesc(text: CharSequence) {
        tvPhotographDesc.text = text
    }

    fun setIDCardImage(data: Any) {
        GlideApp.with(context)
            .load(data)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            //.override(Target.SIZE_ORIGINAL)
            .placeholder(idCardDefault)
            .error(idCardDefault)
            .into(ivIdCard)

        changeViewVisibility(true)
    }

    fun clearIDCardImage() {
        idCardDefault?.let { setIDCardDefault(it) }

        changeViewVisibility(false)
    }

    private fun changeViewVisibility(showDelete: Boolean) {
        if (showDelete) {
            ivPhotograph.visibility = View.GONE
            tvPhotographDesc.visibility = View.GONE

            ivDeleteBg.visibility = View.VISIBLE
            ivDelete.visibility = View.VISIBLE
        } else {
            ivPhotograph.visibility = View.VISIBLE
            tvPhotographDesc.visibility = View.VISIBLE

            ivDeleteBg.visibility = View.GONE
            ivDelete.visibility = View.GONE
        }
    }

    fun setOnViewClickListener(listener: OnViewClickListener) {
        this.onViewClickListener = listener
    }

    interface OnViewClickListener {

        fun onClickDelete()
    }
}
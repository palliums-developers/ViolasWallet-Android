package com.violas.wallet.widget.status

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import kotlinx.android.synthetic.main.widget_data_load_status.view.*

/**
 * Created by elephant on 2019-08-01 18:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 默认的状态布局，负责展现列表空数据和列表加载失败时的UI
 */
class DefaultStatusLayout : FrameLayout, IStatusLayout, View.OnClickListener {

    var onReloadListener: OnReloadListener? = null

    private val mIconResMap: HashMap<Int, Drawable> by lazy { HashMap<Int, Drawable>(3) }
    private val mTextResMap: HashMap<Int, String> by lazy { HashMap<Int, String>(3) }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        View.inflate(context, R.layout.widget_data_load_status, this)
        //        clStatusRoot.setOnClickListener(this)
        //        ivStatusIcon.setOnClickListener(this)
        //        tvStatusTip.setOnClickListener(this)
        //        tvStatusReload.setOnClickListener(this)
        tvStatusReload.visibility = View.GONE

        var status = IStatusLayout.Status.STATUS_NONE
        var failTips = context.getString(R.string.data_load_status_failed)
        var emptyTips = context.getString(R.string.data_load_status_empty)
        var noNetWorkTips = context.getString(R.string.data_load_status_no_network)
        attrs?.let { it ->
            val typedArray =
                context.obtainStyledAttributes(
                    it,
                    R.styleable.DefaultStatusLayout,
                    defStyleAttr,
                    defStyleRes
                )

            val failIcon = typedArray.getDrawable(R.styleable.DefaultStatusLayout_lsFailIcon)
            val emptyIcon = typedArray.getDrawable(R.styleable.DefaultStatusLayout_lsEmptyIcon)

            if (typedArray.hasValue(R.styleable.DefaultStatusLayout_lsFailTip)) {
                failTips = typedArray.getString(R.styleable.DefaultStatusLayout_lsFailTip)!!
            }
            if (typedArray.hasValue(R.styleable.DefaultStatusLayout_lsEmptyTip)) {
                emptyTips = typedArray.getString(R.styleable.DefaultStatusLayout_lsEmptyTip)!!
            }

            status = typedArray.getInt(R.styleable.DefaultStatusLayout_lsStatus, status)

            emptyIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, this) }
            failIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_FAILURE, this) }

            typedArray.recycle()
        }

        setTipWithStatus(IStatusLayout.Status.STATUS_FAILURE, failTips)
        setTipWithStatus(IStatusLayout.Status.STATUS_EMPTY, emptyTips)
        setTipWithStatus(IStatusLayout.Status.STATUS_NO_NETWORK, noNetWorkTips)
        showStatus(status)
    }

    override fun onClick(v: View?) {
        if (BaseActivity.isFastMultiClick(v))
            return

        when (v?.id) {
            R.id.tvStatusReload -> {
                onReloadListener?.onReload()
            }
        }
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, iconRes: Int) {
        val icon: Drawable? = ResourcesCompat.getDrawable(resources, iconRes, null)
        icon?.let { setImageWithStatus(status, it) }
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, icon: Drawable) {
        mIconResMap[status] = icon
    }

    override fun setTipWithStatus(@IStatusLayout.Status status: Int, tipRes: Int) {
        setTipWithStatus(status, context.getString(tipRes))
    }

    override fun setTipWithStatus(@IStatusLayout.Status status: Int, tip: String) {
        mTextResMap[status] = tip
    }

    override fun showStatus(@IStatusLayout.Status status: Int, errorMsg: String?) {
        when (status) {
            IStatusLayout.Status.STATUS_NONE -> {
                visibility = View.GONE
            }

            IStatusLayout.Status.STATUS_EMPTY -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[IStatusLayout.Status.STATUS_EMPTY]
                val icon: Drawable? = mIconResMap[IStatusLayout.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }

            IStatusLayout.Status.STATUS_FAILURE -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[IStatusLayout.Status.STATUS_FAILURE]
                val icon: Drawable? = mIconResMap[IStatusLayout.Status.STATUS_FAILURE]
                    ?: mIconResMap[IStatusLayout.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }

            IStatusLayout.Status.STATUS_NO_NETWORK -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[IStatusLayout.Status.STATUS_NO_NETWORK]
                val icon: Drawable? = mIconResMap[IStatusLayout.Status.STATUS_FAILURE]
                    ?: mIconResMap[IStatusLayout.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }
        }
    }

    interface OnReloadListener {

        fun onReload()
    }
}
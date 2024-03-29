package com.palliums.widget.status

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.widget.FrameLayout
import com.palliums.R
import com.palliums.utils.getDrawable
import com.palliums.utils.getString
import com.palliums.utils.isFastMultiClick
import kotlinx.android.synthetic.main.widget_status_layout.view.*

/**
 * Created by elephant on 2019-08-01 18:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 默认的状态布局，负责展现列表空数据和列表加载失败时的UI
 */
class DefaultStatusLayout : FrameLayout, IStatusLayout, View.OnClickListener {

    private var mOnReloadListener: OnReloadListener? = null

    private val mTipsArr by lazy { SparseArray<String>(3) }
    private val mImageArr by lazy { SparseArray<Drawable>(3) }

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
        View.inflate(context, R.layout.widget_status_layout, this)

        vStatusReload.setOnClickListener(this)

        var status = IStatusLayout.Status.STATUS_NONE
        var emptyTips = context.getString(R.string.common_status_empty)
        var failureTips = context.getString(R.string.common_status_failure)
        var noNetWorkTips = context.getString(R.string.common_status_no_network)

        attrs?.let { it ->
            val typedArray =
                context.obtainStyledAttributes(
                    it,
                    R.styleable.StatusLayout,
                    defStyleAttr,
                    defStyleRes
                )

            status = typedArray.getInt(R.styleable.StatusLayout_slStatus, status)
            if (typedArray.hasValue(R.styleable.StatusLayout_slFailureTip)) {
                failureTips = typedArray.getString(R.styleable.StatusLayout_slFailureTip)!!
            }
            if (typedArray.hasValue(R.styleable.StatusLayout_slNoNetworkTip)) {
                emptyTips = typedArray.getString(R.styleable.StatusLayout_slNoNetworkTip)!!
            }

            val emptyIcon = typedArray.getDrawable(R.styleable.StatusLayout_slEmptyIcon)
            val failureIcon = typedArray.getDrawable(R.styleable.StatusLayout_slFailureIcon)
            val noNetworkIcon = typedArray.getDrawable(R.styleable.StatusLayout_slNoNetworkIcon)

            emptyIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, this) }
            failureIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_FAILURE, this) }
            noNetworkIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_NO_NETWORK, this) }

            typedArray.recycle()
        }

        setTipsWithStatus(IStatusLayout.Status.STATUS_EMPTY, emptyTips)
        setTipsWithStatus(IStatusLayout.Status.STATUS_FAILURE, failureTips)
        setTipsWithStatus(IStatusLayout.Status.STATUS_NO_NETWORK, noNetWorkTips)
        showStatus(status)
    }

    override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            when (view.id) {
                R.id.vStatusReload -> {
                    mOnReloadListener?.onReload()
                }
            }
        }
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, imageRes: Int) {
        getDrawable(imageRes)?.let {
            setImageWithStatus(status, it)
        }
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, image: Drawable) {
        mImageArr.put(status, image)
    }

    override fun setTipsWithStatus(@IStatusLayout.Status status: Int, tipsRes: Int) {
        setTipsWithStatus(status, getString(tipsRes))
    }

    override fun setTipsWithStatus(@IStatusLayout.Status status: Int, tips: String) {
        mTipsArr.put(status, tips)
    }

    override fun showStatus(@IStatusLayout.Status status: Int, errorMsg: String?) {
        when (status) {
            IStatusLayout.Status.STATUS_NONE -> {
                visibility = GONE
            }

            IStatusLayout.Status.STATUS_EMPTY -> {
                visibility = VISIBLE

                val tips: String? = mTipsArr[IStatusLayout.Status.STATUS_EMPTY]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)
            }

            IStatusLayout.Status.STATUS_FAILURE -> {
                visibility = View.VISIBLE

                val tips: String? = mTipsArr[IStatusLayout.Status.STATUS_FAILURE]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_FAILURE]
                    ?: mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)
            }

            IStatusLayout.Status.STATUS_NO_NETWORK -> {
                visibility = View.VISIBLE

                val tips: String? = mTipsArr[IStatusLayout.Status.STATUS_NO_NETWORK]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_NO_NETWORK]
                    ?: mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)
            }
        }
    }

    private fun setStatusData(tips: String?, image: Drawable?) {
        if (image == null) {
            vStatusIcon.visibility = GONE
            vStatusTips.visibility = GONE
            vStatusTipsNoImage.visibility = View.VISIBLE
            tips?.let { vStatusTipsNoImage.text = it }
        } else {
            vStatusIcon.visibility = VISIBLE
            vStatusTips.visibility = VISIBLE
            vStatusTipsNoImage.visibility = View.GONE
            tips?.let { vStatusTips.text = it }
            vStatusIcon.setImageDrawable(image)
        }
    }

    fun setOnReloadListener(listener: OnReloadListener) {
        this.mOnReloadListener = listener
    }

    interface OnReloadListener {

        fun onReload()
    }
}
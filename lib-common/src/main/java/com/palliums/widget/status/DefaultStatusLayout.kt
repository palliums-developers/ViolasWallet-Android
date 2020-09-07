package com.palliums.widget.status

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.palliums.R
import com.palliums.utils.*
import com.scwang.smartrefresh.layout.internal.ProgressDrawable
import kotlinx.android.synthetic.main.layout_loading.view.*
import kotlinx.android.synthetic.main.widget_status_layout.view.*

/**
 * Created by elephant on 2019-08-01 18:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 默认的状态布局，负责展现列表空数据和列表加载失败时的UI
 */
class DefaultStatusLayout : FrameLayout, IStatusLayout, View.OnClickListener {

    private var mLastStatus = IStatusLayout.Status.STATUS_NONE
    private var mShowReloadOnFailed = false

    private var mReloadCallback: (() -> Unit)? = null

    private val mProgressDrawable by lazy {
        ProgressDrawable().apply {
            setColor(getColor(R.color.color_666666))
        }
    }

    private val mTipsArr by lazy { SparseArray<CharSequence>(3) }
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
        vStatusTips.setOnClickListener(this)
        vStatusTipsNoImage.setOnClickListener(this)
        clStatusRoot.setOnClickListener(this)

        var status = IStatusLayout.Status.STATUS_NONE
        var emptyTips = getString(R.string.common_status_empty, context = context)
        var failureTips = getString(R.string.common_status_failure, context = context)
        var noNetWorkTips = getString(R.string.common_status_no_network, context = context)

        var tipsTextSize = DensityUtility.sp2px(context, 14f).toInt()
        var tipsTextColor = getColor(R.color.black_30, context = context)

        var reloadTextSize = tipsTextSize
        var reloadTextColor = tipsTextColor
        var reloadText = getString(R.string.common_status_click_reload, context = context)

        attrs?.let { it ->
            val typedArray =
                context.obtainStyledAttributes(
                    it,
                    R.styleable.StatusLayout,
                    defStyleAttr,
                    defStyleRes
                )

            status = typedArray.getInt(R.styleable.StatusLayout_slStatus, status)
            if (typedArray.hasValue(R.styleable.StatusLayout_slEmptyTip)) {
                emptyTips = typedArray.getString(R.styleable.StatusLayout_slEmptyTip)!!
            }
            if (typedArray.hasValue(R.styleable.StatusLayout_slFailureTip)) {
                failureTips = typedArray.getString(R.styleable.StatusLayout_slFailureTip)!!
            }
            if (typedArray.hasValue(R.styleable.StatusLayout_slNoNetworkTip)) {
                noNetWorkTips = typedArray.getString(R.styleable.StatusLayout_slNoNetworkTip)!!
            }

            val emptyIcon = typedArray.getDrawable(R.styleable.StatusLayout_slEmptyIcon)
            val failureIcon = typedArray.getDrawable(R.styleable.StatusLayout_slFailureIcon)
            val noNetworkIcon = typedArray.getDrawable(R.styleable.StatusLayout_slNoNetworkIcon)

            emptyIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, this) }
            failureIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_FAILURE, this) }
            noNetworkIcon?.run { setImageWithStatus(IStatusLayout.Status.STATUS_NO_NETWORK, this) }

            tipsTextSize = typedArray.getDimensionPixelSize(
                R.styleable.StatusLayout_slTipsTextSize, tipsTextSize
            )
            tipsTextColor = typedArray.getColor(
                R.styleable.StatusLayout_slTipsTextColor, tipsTextColor
            )

            reloadTextSize = typedArray.getDimensionPixelSize(
                R.styleable.StatusLayout_slReloadTextSize, reloadTextSize
            )
            reloadTextColor = typedArray.getColor(
                R.styleable.StatusLayout_slReloadTextColor, reloadTextColor
            )
            if (typedArray.hasValue(R.styleable.StatusLayout_slReloadText)) {
                reloadText = typedArray.getString(R.styleable.StatusLayout_slReloadText)!!
            }
            mShowReloadOnFailed = typedArray.getBoolean(
                R.styleable.StatusLayout_slShowReloadOnFailed, mShowReloadOnFailed
            )

            typedArray.recycle()
        }

        setTipsWithStatus(IStatusLayout.Status.STATUS_EMPTY, emptyTips)
        setTipsWithStatus(IStatusLayout.Status.STATUS_FAILURE, failureTips)
        setTipsWithStatus(IStatusLayout.Status.STATUS_NO_NETWORK, noNetWorkTips)
        setTipsTextSize(tipsTextSize)
        setTipsTextColor(tipsTextColor)
        setReloadTextSize(reloadTextSize)
        setReloadTextColor(reloadTextColor)
        setReloadText(reloadText)
        showStatus(status)
    }

    override fun onClick(view: View) {
        if (!isFastMultiClick(view)) {
            when (view.id) {
                R.id.vStatusReload, R.id.vStatusTips, R.id.vStatusTipsNoImage -> {
                    // do not process
                }

                R.id.clStatusRoot -> {
                    if (mLastStatus == IStatusLayout.Status.STATUS_FAILURE
                        || mLastStatus == IStatusLayout.Status.STATUS_NO_NETWORK
                    ) {
                        mReloadCallback?.invoke()
                    }
                }
            }
        }
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, imageRes: Int): IStatusLayout {
        getDrawableCompat(imageRes, context)?.let {
            setImageWithStatus(status, it)
        }
        return this
    }

    override fun setImageWithStatus(@IStatusLayout.Status status: Int, image: Drawable): IStatusLayout {
        mImageArr.put(status, image)
        return this
    }

    override fun setTipsWithStatus(@IStatusLayout.Status status: Int, tipsRes: Int): IStatusLayout {
        setTipsWithStatus(status, getString(tipsRes, context = context))
        return this
    }

    override fun setTipsWithStatus(@IStatusLayout.Status status: Int, tips: CharSequence): IStatusLayout {
        mTipsArr.put(status, tips)
        return this
    }

    override fun showStatus(@IStatusLayout.Status status: Int, errorMsg: CharSequence?) {
        when (status) {
            IStatusLayout.Status.STATUS_NONE -> {
                visibility = GONE
                hideLoading()
            }

            IStatusLayout.Status.STATUS_LOADING -> {
                visibility = VISIBLE
                vStatusIcon.visibility = GONE
                vStatusTips.visibility = GONE
                vStatusTipsNoImage.visibility = GONE
                vStatusReload.visibility = GONE

                showLoading()
            }

            IStatusLayout.Status.STATUS_EMPTY -> {
                visibility = VISIBLE
                hideLoading()
                vStatusReload.visibility = GONE

                val tips: CharSequence? = mTipsArr[IStatusLayout.Status.STATUS_EMPTY]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)
            }

            IStatusLayout.Status.STATUS_FAILURE -> {
                visibility = View.VISIBLE
                hideLoading()

                val tips: CharSequence? = mTipsArr[IStatusLayout.Status.STATUS_FAILURE]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_FAILURE]
                    ?: mImageArr[IStatusLayout.Status.STATUS_NO_NETWORK]
                    ?: mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)

                if (mShowReloadOnFailed) {
                    vStatusReload.visibility = View.VISIBLE
                }
            }

            IStatusLayout.Status.STATUS_NO_NETWORK -> {
                visibility = View.VISIBLE
                hideLoading()

                val tips: CharSequence? = mTipsArr[IStatusLayout.Status.STATUS_NO_NETWORK]
                val image: Drawable? = mImageArr[IStatusLayout.Status.STATUS_NO_NETWORK]
                    ?: mImageArr[IStatusLayout.Status.STATUS_FAILURE]
                    ?: mImageArr[IStatusLayout.Status.STATUS_EMPTY]
                setStatusData(tips, image)

                if (mShowReloadOnFailed) {
                    vStatusReload.visibility = View.VISIBLE
                }
            }
        }

        mLastStatus = status
    }

    override fun setTipsTextSize(size: Int): IStatusLayout {
        vStatusTips.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        vStatusTipsNoImage.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        return this
    }

    override fun setTipsTextColor(color: Int): IStatusLayout {
        vStatusTips.setTextColor(color)
        vStatusTipsNoImage.setTextColor(color)
        return this
    }

    override fun setReloadText(resId: Int): IStatusLayout {
        vStatusReload.setText(resId)
        return this
    }

    override fun setReloadText(text: String): IStatusLayout {
        vStatusReload.text = text
        return this
    }

    override fun setReloadTextSize(size: Int): IStatusLayout {
        vStatusReload.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        return this
    }

    override fun setReloadTextColor(color: Int): IStatusLayout {
        vStatusReload.setTextColor(color)
        return this
    }

    override fun getReloadTextView(): TextView {
        return vStatusReload
    }

    override fun setShowReloadOnFailed(show: Boolean): IStatusLayout {
        mShowReloadOnFailed = show
        return this
    }

    override fun setReloadCallback(callback: () -> Unit): IStatusLayout {
        this.mReloadCallback = callback
        return this
    }

    private fun setStatusData(tips: CharSequence?, image: Drawable?) {
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

    private fun showLoading() {
        if (ivProgress.drawable == null) {
            ivProgress.setImageDrawable(mProgressDrawable)
        }
        mProgressDrawable.start()
        clLoading.visibility = VISIBLE
    }

    private fun hideLoading() {
        clLoading.visibility = GONE
        if (mLastStatus == IStatusLayout.Status.STATUS_LOADING) {
            mProgressDrawable.stop()
        }
    }
}
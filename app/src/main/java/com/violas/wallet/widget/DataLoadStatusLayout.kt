package com.violas.wallet.widget

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
 * desc: DataLoadStatusLayout，负责展现列表空数据和列表加载失败时的UI
 */
class DataLoadStatusLayout : FrameLayout, DataLoadStatusControl, View.OnClickListener {

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

        var status = DataLoadStatusControl.Status.STATUS_NONE
        var failTips = context.getString(R.string.data_load_status_failed)
        var emptyTips = context.getString(R.string.data_load_status_empty)
        var noNetWorkTips = context.getString(R.string.data_load_status_no_network)
        attrs?.let { it ->
            val typedArray =
                context.obtainStyledAttributes(
                    it,
                    R.styleable.DataLoadStatusLayout,
                    defStyleAttr,
                    defStyleRes
                )

            val failIcon = typedArray.getDrawable(R.styleable.DataLoadStatusLayout_lsFailIcon)
            val emptyIcon = typedArray.getDrawable(R.styleable.DataLoadStatusLayout_lsEmptyIcon)

            if (typedArray.hasValue(R.styleable.DataLoadStatusLayout_lsFailTip)) {
                failTips = typedArray.getString(R.styleable.DataLoadStatusLayout_lsFailTip)!!
            }
            if (typedArray.hasValue(R.styleable.DataLoadStatusLayout_lsEmptyTip)) {
                emptyTips = typedArray.getString(R.styleable.DataLoadStatusLayout_lsEmptyTip)!!
            }

            status = typedArray.getInt(R.styleable.DataLoadStatusLayout_lsStatus, status)

            emptyIcon?.run { setImageWithStatus(DataLoadStatusControl.Status.STATUS_EMPTY, this) }
            failIcon?.run { setImageWithStatus(DataLoadStatusControl.Status.STATUS_FAIL, this) }

            typedArray.recycle()
        }

        setTipWithStatus(DataLoadStatusControl.Status.STATUS_FAIL, failTips)
        setTipWithStatus(DataLoadStatusControl.Status.STATUS_EMPTY, emptyTips)
        setTipWithStatus(DataLoadStatusControl.Status.STATUS_NO_NETWORK, noNetWorkTips)
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

    override fun setImageWithStatus(@DataLoadStatusControl.Status status: Int, iconRes: Int) {
        val icon: Drawable? = ResourcesCompat.getDrawable(resources, iconRes, null)
        icon?.let { setImageWithStatus(status, it) }
    }

    override fun setImageWithStatus(@DataLoadStatusControl.Status status: Int, icon: Drawable) {
        mIconResMap[status] = icon
    }

    override fun setTipWithStatus(@DataLoadStatusControl.Status status: Int, tipRes: Int) {
        setTipWithStatus(status, context.getString(tipRes))
    }

    override fun setTipWithStatus(@DataLoadStatusControl.Status status: Int, tip: String) {
        mTextResMap[status] = tip
    }

    override fun showStatus(@DataLoadStatusControl.Status status: Int, errorMsg: String?) {
        when (status) {
            DataLoadStatusControl.Status.STATUS_NONE -> {
                visibility = View.GONE
            }

            DataLoadStatusControl.Status.STATUS_EMPTY -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[DataLoadStatusControl.Status.STATUS_EMPTY]
                val icon: Drawable? = mIconResMap[DataLoadStatusControl.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }

            DataLoadStatusControl.Status.STATUS_FAIL -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[DataLoadStatusControl.Status.STATUS_FAIL]
                val icon: Drawable? = mIconResMap[DataLoadStatusControl.Status.STATUS_FAIL]
                    ?: mIconResMap[DataLoadStatusControl.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }

            DataLoadStatusControl.Status.STATUS_NO_NETWORK -> {
                visibility = View.VISIBLE

                val tip: String? = mTextResMap[DataLoadStatusControl.Status.STATUS_NO_NETWORK]
                val icon: Drawable? = mIconResMap[DataLoadStatusControl.Status.STATUS_FAIL]
                    ?: mIconResMap[DataLoadStatusControl.Status.STATUS_EMPTY]

                tip?.let { tvStatusTip.text = it }
                icon?.let { ivStatusIcon.setImageDrawable(it) }
            }
        }
    }

    interface OnReloadListener {

        fun onReload()
    }
}
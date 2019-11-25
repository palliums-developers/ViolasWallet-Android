package com.violas.wallet.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.violas.wallet.R
import kotlinx.android.synthetic.main.view_apply_submit_item.view.*

class ApplySubmitItemView : LinearLayout {
    private var content: String? = null
    private var title: String? = null
    private var view: View? = null
    private var attrs: AttributeSet? = null
    private var defStyleAttr: Int = 0
    private var defStyleRes: Int = 0

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
        view =
            LayoutInflater.from(context).inflate(R.layout.view_apply_submit_item, this, false)
        addView(view)

        view?.apply {
            tvContent.text = content
            tvTitle.text = title
        }
    }

    private fun loadDefValue() {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ApplySubmitItemView
        )
        content = typedArray.getString(R.styleable.ApplySubmitItemView_item_content)
        title = typedArray.getString(R.styleable.ApplySubmitItemView_item_title)
        typedArray.recycle()
    }

    fun setContent(content: String) {
        view?.apply {
            tvContent.text = content
        }
    }
}
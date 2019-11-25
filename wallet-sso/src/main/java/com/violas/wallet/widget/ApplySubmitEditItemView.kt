package com.violas.wallet.widget

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.violas.wallet.R
import kotlinx.android.synthetic.main.view_apply_submit_item.view.*

class ApplySubmitEditItemView : LinearLayout {
    private var content: String? = null
    private var title: String? = null
    private var hint: String? = null
    private var inputType: Int? = null
    private var maxLength: Int? = null
    private var digits: String? = null
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
            LayoutInflater.from(context).inflate(R.layout.view_apply_submit_edit_item, this, false)
        addView(view)

        view?.apply {
            tvContent.text = content
            tvTitle.text = title
            tvContent.hint = hint
            inputType?.let { tvContent.inputType = it }
            maxLength?.let {
                tvContent.filters = arrayOf(
                    InputFilter.LengthFilter(
                        it
                    )
                )
            }
            digits?.let { tvContent.keyListener = DigitsKeyListener.getInstance(it) }
        }
    }

    private fun loadDefValue() {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ApplySubmitEditItemView
        )
        content = typedArray.getString(R.styleable.ApplySubmitEditItemView_item_content)
        title = typedArray.getString(R.styleable.ApplySubmitEditItemView_item_title)
        hint = typedArray.getString(R.styleable.ApplySubmitEditItemView_item_hint)
        inputType = typedArray.getInt(
            R.styleable.ApplySubmitEditItemView_android_inputType,
            InputType.TYPE_CLASS_TEXT
        )
        digits = typedArray.getString(R.styleable.ApplySubmitEditItemView_android_digits)
        maxLength = typedArray.getInt(
            R.styleable.ApplySubmitEditItemView_android_maxLength, Int.MAX_VALUE
        )
        typedArray.recycle()
    }

    fun setContent(content: String) {
        view?.apply {
            tvContent.text = content
        }
    }

    fun setHint(hint: String) {
        view?.apply {
            tvContent.hint = hint
        }
    }
}
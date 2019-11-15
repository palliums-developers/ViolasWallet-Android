package com.violas.wallet.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Created by elephant on 2019-11-14 16:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MarqueTextView : AppCompatTextView {

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    override fun isFocused(): Boolean {
        return true
    }
}
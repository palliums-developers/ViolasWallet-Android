package com.violas.wallet.widget.refresh

import android.content.Context
import android.util.AttributeSet
import com.scwang.smartrefresh.layout.SmartRefreshLayout

/**
 * Created by elephant on 2019-11-06 14:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 默认的刷新布局
 */
class DefaultRefreshLayout : SmartRefreshLayout, IRefreshLayout {

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
}
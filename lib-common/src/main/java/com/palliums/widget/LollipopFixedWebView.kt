package com.palliums.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.webkit.WebView

/**
 * Created by elephant on 2020-02-03 18:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Override WebView for fix bug
 * bug desc: 在Android5.0或5.1上，及androidx appcompat 1.1.0版本，
 * 在xml中引用 WebView 出现 android.content.res.Resources$NotFoundException
 *
 * @see <a href="https://github.com/SheHuan/NiceImageView">link</a>
 */
class LollipopFixedWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context.createConfigurationContext(Configuration()), attrs, defStyleAttr, defStyleRes)
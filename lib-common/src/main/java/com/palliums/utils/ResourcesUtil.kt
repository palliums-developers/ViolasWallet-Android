package com.palliums.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.palliums.content.ContextProvider

/**
 * Created by elephant on 2019-11-14 11:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@JvmOverloads
fun getString(
    @StringRes res: Int,
    vararg formatArgs: Any,
    context: Context = ContextProvider.getContext()
): String {
    return context.getString(res, formatArgs)
}

@ColorInt
@JvmOverloads
fun getColor(
    @ColorRes res: Int,
    context: Context = ContextProvider.getContext()
): Int {
    return ResourcesCompat.getColor(context.resources, res, null)
}

@JvmOverloads
fun getDrawable(
    @DrawableRes res: Int,
    context: Context = ContextProvider.getContext()
): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, res, null)
}
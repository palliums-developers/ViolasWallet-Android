package com.palliums.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.TypedValue
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
    @StringRes resId: Int,
    vararg formatArgs: Any,
    context: Context = ContextProvider.getContext()
): String {
    return context.getString(resId, *formatArgs)
}

@JvmOverloads
fun getString(
    @StringRes resId: Int,
    context: Context = ContextProvider.getContext()
): String {
    return context.getString(resId)
}

@ColorInt
@JvmOverloads
fun getColor(
    @ColorRes resId: Int,
    context: Context = ContextProvider.getContext(),
    theme: Resources.Theme? = null
): Int {
    return ResourcesCompat.getColor(context.resources, resId, theme ?: context.theme)
}

@ColorInt
fun getColorByAttrId(
    attrId: Int,
    context: Context
): Int {
    var typedArray: TypedArray? = null
    return try {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrId, typedValue, true)
        typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(attrId))
        typedArray.getColor(0, 0x000000)
    } catch (ignore: Exception) {
        0
    } finally {
        typedArray?.recycle()
    }
}

@JvmOverloads
fun getDrawableCompat(
    @DrawableRes resId: Int,
    context: Context,
    theme: Resources.Theme? = null
): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, resId, theme ?: context.theme)
}

fun getDrawableByAttrId(
    attrId: Int,
    context: Context
): Drawable? {
    var typedArray: TypedArray? = null
    return try {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrId, typedValue, true)
        typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(attrId))
        typedArray.getDrawable(0)
    } catch (ignore: Exception) {
        null
    } finally {
        typedArray?.recycle()
    }
}

fun getResourceId(
    attrId: Int,
    context: Context
): Int {
    return try {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrId, typedValue, true)
        typedValue.resourceId
    } catch (ignore: Exception) {
        0
    }
}
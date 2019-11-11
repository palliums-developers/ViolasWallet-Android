package com.violas.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat

@SuppressLint("StaticFieldLeak")
object ContextProvider {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
    }

    fun getContext(): Context {
        return context
    }
}

fun getContext(): Context {
    return ContextProvider.getContext().applicationContext!!
}

@JvmOverloads
fun getString(@StringRes res: Int, context: Context = getContext()): String {
    return context.getString(res)
}

@ColorInt
@JvmOverloads
fun getColor(@ColorRes res: Int, context: Context = getContext()): Int {
    return ResourcesCompat.getColor(context.resources, res, null)
}

@Override
@JvmOverloads
fun getDrawable(@DrawableRes res: Int, context: Context = getContext()): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, res, null)
}


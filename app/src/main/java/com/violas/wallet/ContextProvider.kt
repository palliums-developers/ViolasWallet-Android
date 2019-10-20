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

fun getString(@StringRes res: Int): String {
    return getContext().getString(res)
}

@ColorInt
fun getColor(@ColorRes res: Int): Int {
    return ResourcesCompat.getColor(getContext().resources, res, null)
}

fun getDrawable(@DrawableRes res: Int): Drawable? {
    return ResourcesCompat.getDrawable(getContext().resources, res, null)
}


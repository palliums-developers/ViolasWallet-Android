package com.violas.wallet.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

fun loadTransform(
    context: Context,
    @DrawableRes placeholderId: Int,
    radius: Int
): RequestBuilder<Drawable?>? {
    return Glide.with(context)
        .load(placeholderId)
        .apply(RequestOptions.bitmapTransform(RoundedCorners(radius)))
}
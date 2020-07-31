package com.violas.wallet.utils

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.palliums.utils.DensityUtility

fun ImageView.loadRoundedImage(
    imageRes: String,
    @DrawableRes placeholderId: Int,
    radiusDp: Int
) {
    val radiusPx = DensityUtility.dp2px(this.context, radiusDp)
    Glide.with(this)
        .load(imageRes)
        .error(placeholderId)
        .placeholder(placeholderId)
        .thumbnail(
            Glide.with(this)
                .load(placeholderId)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(radiusPx)))
        )
        .apply(RequestOptions.bitmapTransform(RoundedCorners(radiusPx)))
        .into(this)
}

fun ImageView.loadCircleImage(
    imageRes: String,
    @DrawableRes placeholderId: Int
) {
    Glide.with(this)
        .load(imageRes)
        .error(placeholderId)
        .placeholder(placeholderId)
        .thumbnail(
            Glide.with(this)
                .load(placeholderId)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
        )
        .apply(RequestOptions.bitmapTransform(CircleCrop()))
        .into(this)
}
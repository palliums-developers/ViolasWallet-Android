package com.violas.wallet.image

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.lxj.xpopup.interfaces.XPopupImageLoader
import com.violas.wallet.R
import java.io.File

/**
 * Created by elephant on 2020/5/9 11:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ImageLoader : XPopupImageLoader {

    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        GlideApp.with(imageView)
            .load(uri)
            .placeholder(-1)
            .override(Target.SIZE_ORIGINAL)
            .into(imageView)
    }

    override fun getImageFile(context: Context, uri: Any): File? {
       return try {
            GlideApp.with(context)
                .downloadOnly()
                .load(uri)
                .submit()
                .get()
        } catch (e: Exception) {
            e.printStackTrace()
           null
        }
    }
}
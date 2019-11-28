package com.violas.wallet.image

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream

/**
 * Created by elephant on 2019-08-12 15:08.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Gilde配置文件，配置后可以使用GlideApp.with Glide.with
 *
 */
@GlideModule
class GlideConfiguration : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 10 * 1024 * 1024))

        val memorySizeCalculator = MemorySizeCalculator.Builder(context).build()

        val defaultMemoryCacheSize = memorySizeCalculator.memoryCacheSize
        val customMemoryCacheSize = (defaultMemoryCacheSize * 1.2).toLong()
        builder.setMemoryCache(LruResourceCache(customMemoryCacheSize))

        val defaultBitmapPoolSize = memorySizeCalculator.bitmapPoolSize
        val customBitmapPoolSize = (defaultBitmapPoolSize * 1.2).toLong()
        builder.setBitmapPool(LruBitmapPool(customBitmapPoolSize))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(HttpLoggingInterceptor().apply{
            level = HttpLoggingInterceptor.Level.HEADERS
        })

        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(builder.build())
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        //为了维持对 Glide v3 的 GlideModules 的向后兼容性，
        //Glide 仍然会解析应用程序和所有被包含的库中的 AndroidManifest.xml 文件，
        //并包含在这些清单中列出的旧 GlideModules 模块类。
        //当前用的 Glide v4，禁用清单解析，以改善 Glide 的初始启动时间。
        return false
    }
}
package com.palliums.content

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * Created by elephant on 2019-11-14 11:05.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@SuppressLint("StaticFieldLeak")
object ContextProvider {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = if (context is Application) context else context.applicationContext
    }

    fun getContext(): Context {
        return context
    }
}
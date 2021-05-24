package com.palliums.net

/**
 * Created by elephant on 2019-11-11 09:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ApiResponse {

    fun isSuccess(): Boolean

    fun getErrorMsg(): Any?

    fun getErrorCode(): Any? {
        return null
    }

    fun getResponseData(): Any? {
        return null
    }
}
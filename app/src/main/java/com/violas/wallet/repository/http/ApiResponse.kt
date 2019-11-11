package com.violas.wallet.repository.http

/**
 * Created by elephant on 2019-11-11 09:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ApiResponse {

    fun getSuccessCode(): Any

    fun getErrorCode(): Any

    fun getErrorMsg(): Any?
}
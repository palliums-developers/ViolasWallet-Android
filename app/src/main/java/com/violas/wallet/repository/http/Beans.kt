package com.violas.wallet.repository.http

/**
 * Created by elephant on 2019-11-04 18:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

open class BaseResponse<T> {
    var code: Int = 0
    var message: String? = null
    var data: T? = null
}
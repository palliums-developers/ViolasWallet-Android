package com.violas.wallet.repository.http

import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 2019-11-07 16:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
open class BaseResponse<T> {

    @SerializedName(value = "code", alternate = ["err_no"])
    var code: Int = 0

    @SerializedName(value = "message", alternate = ["err_msg"])
    var message: String? = null

    @SerializedName(value = "data")
    var data: T? = null
}

open class BasePagingResponse<T> : BaseResponse<BasePagingResponse<T>>() {

    @SerializedName(value = "limit", alternate = ["pagesize"])
    var pageSize: Int = 0

    @SerializedName(value = "offset", alternate = ["page"])
    var pageIndex: Int = 0

    @SerializedName(value = "orders", alternate = ["list"])
    var list: List<T>? = null
}
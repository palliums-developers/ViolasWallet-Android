package com.violas.wallet.biz.btc.bean

/**
 * Created by elephant on 5/7/21 11:13 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class Fees(
    val fastestFee: Long,   // in satoshis per byte
    val halfHourFee: Long,  // in satoshis per byte
    val hourFee: Long       // in satoshis per byte
)
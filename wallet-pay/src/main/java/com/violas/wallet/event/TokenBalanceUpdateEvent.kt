package com.violas.wallet.event

/**
 * Created by elephant on 2019-12-27 18:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Token余额更新事件
 */
class TokenBalanceUpdateEvent(
    val accountAddress: String,
    val tokenIdx: Long,
    val tokenBalance: Long
)
package com.violas.wallet.ui.account

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-23 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 账户展示类型
 */
@IntDef(
    AccountDisplayType.ALL,     // 展示所有账户
    AccountDisplayType.VIOLAS,  // 只展示Violas账户
    AccountDisplayType.LIBRA,   // 只展示Libra账户
    AccountDisplayType.BTC      // 只展示Btc账户
)
annotation class AccountDisplayType {

    companion object {
        const val ALL = 0x01
        const val VIOLAS = 0x02
        const val LIBRA = 0x03
        const val BTC = 0x04
    }
}
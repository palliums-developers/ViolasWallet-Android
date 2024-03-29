package com.violas.wallet.ui.account

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-23 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 账户类型
 */
@IntDef(
    AccountType.ALL,     // 所有账户
    AccountType.VIOLAS,  // Violas账户
    AccountType.LIBRA,   // Libra账户
    AccountType.BTC      // Btc账户
)
annotation class AccountType {

    companion object {
        const val ALL = 0x01
        const val VIOLAS = 0x02
        const val LIBRA = 0x03
        const val BTC = 0x04
    }
}
package com.violas.wallet.ui.account

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-23 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 账户操作模式
 */
@IntDef(
    AccountOperationMode.SELECTION,  // 选择模式
    AccountOperationMode.MANAGEMENT  // 管理模式
)
annotation class AccountOperationMode {

    companion object {
        const val SELECTION = 0x01
        const val MANAGEMENT = 0x02
    }
}
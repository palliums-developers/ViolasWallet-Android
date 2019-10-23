package com.violas.wallet.ui.backup

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-21 16:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词来源
 */
@IntDef(
    BackupMnemonicFrom.CREATE_IDENTITY,
    BackupMnemonicFrom.IDENTITY_WALLET,
    BackupMnemonicFrom.OTHER_WALLET
)
annotation class BackupMnemonicFrom {

    companion object {
        const val CREATE_IDENTITY = 0x01    // 创建身份
        const val IDENTITY_WALLET = 0x02    // 身份钱包
        const val OTHER_WALLET = 0x03       // 非身份钱包
    }
}
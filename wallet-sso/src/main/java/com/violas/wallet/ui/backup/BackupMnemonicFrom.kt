package com.violas.wallet.ui.backup

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-21 16:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词来源
 */
@IntDef(
    BackupMnemonicFrom.CREATE_IDENTITY_WALLET,
    BackupMnemonicFrom.BACKUP_IDENTITY_WALLET,
    BackupMnemonicFrom.CREATE_OTHER_WALLET,
    BackupMnemonicFrom.ONLY_SHOW_MNEMONIC
)
annotation class BackupMnemonicFrom {

    companion object {
        const val CREATE_IDENTITY_WALLET = 0x01     // 创建身份钱包
        const val BACKUP_IDENTITY_WALLET = 0x02     // 备份身份钱包
        const val CREATE_OTHER_WALLET = 0x03        // 创建非身份钱包
        const val ONLY_SHOW_MNEMONIC = 0x04         // 仅显示助记词
    }
}
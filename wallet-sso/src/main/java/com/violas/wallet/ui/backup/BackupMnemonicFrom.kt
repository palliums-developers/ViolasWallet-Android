package com.violas.wallet.ui.backup

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-21 16:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词来源
 */
@IntDef(
    BackupMnemonicFrom.CREATE_GOVERNOR_WALLET,
    BackupMnemonicFrom.BACKUP_GOVERNOR_WALLET,
    BackupMnemonicFrom.CREATE_SSO_WALLET,
    BackupMnemonicFrom.BACKUP_SSO_WALLET,
    BackupMnemonicFrom.ONLY_SHOW_MNEMONIC
)
annotation class BackupMnemonicFrom {

    companion object {
        const val CREATE_GOVERNOR_WALLET = 0x01     // 创建州长钱包
        const val BACKUP_GOVERNOR_WALLET = 0x02     // 备份州长钱包
        const val CREATE_SSO_WALLET = 0x03          // 创建SSO钱包
        const val BACKUP_SSO_WALLET = 0x04          // 备份SSO钱包
        const val ONLY_SHOW_MNEMONIC = 0x05         // 仅显示助记词
    }
}
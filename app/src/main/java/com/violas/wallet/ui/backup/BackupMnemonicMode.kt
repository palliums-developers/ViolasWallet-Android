package com.violas.wallet.ui.backup

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-21 16:37.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词模式
 */
@IntDef(BackupMnemonicMode.IDENTITY_WALLET, BackupMnemonicMode.CREATE_WALLET)
annotation class BackupMnemonicMode {

    companion object {
        const val IDENTITY_WALLET = 0x01    //身份钱包，钱包地址私钥已生成并存储
        const val CREATE_WALLET = 0x02      //创建钱包，钱包地址私钥未生成，在确认助记词后存储
    }
}
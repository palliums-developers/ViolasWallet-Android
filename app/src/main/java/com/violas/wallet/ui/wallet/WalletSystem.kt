package com.violas.wallet.ui.wallet

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-10-21 18:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包体系
 */
@IntDef(WalletSystem.VIOLAS, WalletSystem.LIBRA, WalletSystem.BTC)
annotation class WalletSystem {

    companion object {
        const val VIOLAS = 0x01
        const val LIBRA = 0x02
        const val BTC = 0x03
    }
}
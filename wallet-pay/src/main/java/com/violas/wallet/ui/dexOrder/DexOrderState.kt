package com.violas.wallet.ui.dexOrder

import androidx.annotation.StringDef

/**
 * Created by elephant on 2019-12-17 11:45.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
@StringDef(
    DexOrderState.OPEN,
    DexOrderState.FILLED,
    DexOrderState.CANCELLING,
    DexOrderState.CANCELED,
    DexOrderState.FINISHED,
    DexOrderState.UNFINISHED
)
annotation class DexOrderState {
    companion object {
        const val OPEN = "0"        // open
        const val FILLED = "1"      // filled
        const val CANCELLING = "2"  // cancelling
        const val CANCELED = "3"    // canceled
        const val FINISHED = "4"    // finished（filled and canceled）
        const val UNFINISHED = "5"  // unfinished（open and cancelling）
    }
}
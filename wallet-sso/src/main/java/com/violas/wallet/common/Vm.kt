package com.violas.wallet.common

import com.quincysx.crypto.CoinType
import com.violas.wallet.BuildConfig

object Vm {
    @kotlin.jvm.JvmField
    var defBitcoinType: CoinType
    @kotlin.jvm.JvmField
    var TestNet: Boolean = false
    @kotlin.jvm.JvmField
    var Confirmations: Int = 0
    @kotlin.jvm.JvmField
    var mUTXOServiceAddress: String? = null

    //    ext.isTextNet = "testnet"
    //    ext.isTextNet = "devnet"
    //    ext.isTextNet = "main"
    init {
        if (BuildConfig.TESTNET.equals("main")) {
            TestNet = false
            defBitcoinType = CoinType.Bitcoin
            //            Confirmations = 6;
            Confirmations = 1
        } else {
            TestNet = true
            defBitcoinType = CoinType.BitcoinTest
            Confirmations = 1
        }
    }
}

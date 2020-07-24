package com.violas.wallet.common

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig

object Vm {
    @kotlin.jvm.JvmField
    var DefBitcoinType: CoinTypes

    @kotlin.jvm.JvmField
    var TestNet: Boolean = false

    @kotlin.jvm.JvmField
    var Confirmations: Int = 0

    @kotlin.jvm.JvmField
    var mUTXOServiceAddress: String? = null

    @kotlin.jvm.JvmField
    var LibraChainId: Int = 2

    //    ext.isTextNet = "testnet"
    //    ext.isTextNet = "devnet"
    //    ext.isTextNet = "main"
    init {
        if (BuildConfig.TESTNET.equals("main")) {
            TestNet = false
            DefBitcoinType = CoinTypes.Bitcoin
            //            Confirmations = 6;
            Confirmations = 1
        } else {
            TestNet = true
            DefBitcoinType = CoinTypes.BitcoinTest
            Confirmations = 1
        }
    }
}

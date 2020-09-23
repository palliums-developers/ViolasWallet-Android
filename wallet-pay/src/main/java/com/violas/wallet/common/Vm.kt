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

    @kotlin.jvm.JvmField
    var ViolasChainId: Int = 4

    // see more eip155 https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md
    // 1	Ethereum mainnet
    // 2	Morden (disused), Expanse mainnet
    // 3	Ropsten
    // 4	Rinkeby
    // 5	Goerli
    // 42	Kovan
    // 1337	Geth private chains (default)
    @kotlin.jvm.JvmField
    var EthereumChainId: Int = 1

    //    ext.isTextNet = "testnet"
    //    ext.isTextNet = "devnet"
    //    ext.isTextNet = "main"
    init {
        if (BuildConfig.TESTNET.equals("main")) {
            TestNet = false
            DefBitcoinType = CoinTypes.Bitcoin
            //            Confirmations = 6;
            Confirmations = 1
            EthereumChainId = 1
        } else {
            TestNet = true
            DefBitcoinType = CoinTypes.BitcoinTest
            Confirmations = 1
            EthereumChainId = 3
        }
    }
}

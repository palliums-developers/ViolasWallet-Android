package com.violas.wallet.common

import com.quincysx.crypto.CoinType

object VmConfigs {

    @kotlin.jvm.JvmField
    val BITCOIN_COIN_TYPE: CoinType = CoinType.BitcoinTest

    const val BITCOIN_CONFIRMATIONS: Int = 1

    @kotlin.jvm.JvmField
    val BITCOIN_UTXO_URL: String? = null

    @kotlin.jvm.JvmField
    val DIEM_COIN_TYPE: CoinType = CoinType.DiemTest

    const val DIEM_CHAIN_ID: Int = 2

    @kotlin.jvm.JvmField
    val DIEM_NETWORK_PREFIX =
        org.palliums.libracore.wallet.AccountIdentifier.NetworkPrefix.TestnetPrefix

    @kotlin.jvm.JvmField
    val VIOLAS_COIN_TYPE: CoinType = CoinType.ViolasTest

    const val VIOLAS_CHAIN_ID: Int = 4

    @kotlin.jvm.JvmField
    val VIOLAS_NETWORK_PREFIX =
        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.TestnetPrefix
}

package com.violas.wallet.common

import com.quincysx.crypto.CoinType

object VmConfigs {

    @kotlin.jvm.JvmField
    val BITCOIN_COIN_TYPE: CoinType = CoinType.Bitcoin

    const val BITCOIN_CONFIRMATIONS: Int = 6

    @kotlin.jvm.JvmField
    val BITCOIN_UTXO_URL: String? = null

    @kotlin.jvm.JvmField
    val DIEM_COIN_TYPE: CoinType = CoinType.Diem

    const val DIEM_CHAIN_ID: Int = 1

    @kotlin.jvm.JvmField
    val DIEM_NETWORK_PREFIX =
        org.palliums.libracore.wallet.AccountIdentifier.NetworkPrefix.MainnetPrefix

    @kotlin.jvm.JvmField
    val VIOLAS_COIN_TYPE: CoinType = CoinType.Violas

    const val VIOLAS_CHAIN_ID: Int = 1

    @kotlin.jvm.JvmField
    val VIOLAS_NETWORK_PREFIX =
        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.MainnetPrefix
}

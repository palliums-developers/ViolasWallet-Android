package com.violas.wallet.common

import com.quincysx.crypto.CoinType

/**
 * Created by elephant on 2/7/21 12:21 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun isBitcoinTestNet(): Boolean {
    return VmConfigs.BITCOIN_COIN_TYPE == CoinType.BitcoinTest
}

fun getBitcoinCoinType(): CoinType {
    return VmConfigs.BITCOIN_COIN_TYPE
}

fun getBitcoinConfirmations(): Int {
    return VmConfigs.BITCOIN_CONFIRMATIONS
}

fun isDiemTestNet(): Boolean {
    return VmConfigs.DIEM_COIN_TYPE == CoinType.DiemTest
}

fun getDiemCoinType(): CoinType {
    return VmConfigs.DIEM_COIN_TYPE
}

fun getDiemChainId(): Int {
    return VmConfigs.DIEM_CHAIN_ID
}

fun geDiemNetworkPrefix(): org.palliums.libracore.wallet.AccountIdentifier.NetworkPrefix {
    return VmConfigs.DIEM_NETWORK_PREFIX
}

fun isViolasTestNet(): Boolean {
    return VmConfigs.VIOLAS_COIN_TYPE == CoinType.ViolasTest
}

fun getViolasCoinType(): CoinType {
    return VmConfigs.VIOLAS_COIN_TYPE
}

fun getViolasChainId(): Int {
    return VmConfigs.VIOLAS_CHAIN_ID
}

fun getViolasNetworkPrefix(): org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix {
    return VmConfigs.VIOLAS_NETWORK_PREFIX
}

fun getEthereumCoinType(): CoinType {
    return CoinType.Ethereum
}
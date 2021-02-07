package com.violas.wallet.common

object ApiBaseUrl {

    // Violas base url
    const val VIOLAS_BASE_URL = "https://api.violas.io"

    // Violas dex base url
    const val VIOLAS_DEX_BASE_URL = "https://dex.violas.io"

    // Violas chain base url
    const val VIOLAS_CHAIN_BASE_URL = "https://ac.testnet.violas.io"

    // Libra chain base url
    const val LIBRA_CHAIN_BASE_URL = "https://client.testnet.libra.org"

    // Libexplorer base url
    const val LIBEXPLORER_BASE_URL = "https://api-test.libexplorer.com/api"

    // Bitmain base url
    // 对外公开，有API速率限制（每分钟120次）
    const val BITMAIN_OPEN_BASE_URL = "https://chain.api.btc.com/v3"
    // 正式对接，无API速率限制
    const val BITMAIN_BASE_URL = "https://developer-btc-chain.api.btc.com/appkey-e6e2ce95d8df"

    // Trezor base url
    const val TREZOR_BASE_URL = "https://tbtc1.trezor.io/api"
}
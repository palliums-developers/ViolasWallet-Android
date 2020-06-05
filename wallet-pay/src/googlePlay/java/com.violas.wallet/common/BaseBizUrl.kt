package com.violas.wallet.common

object BaseBizUrl {

    // Violas base url
    private const val VIOLAS_BASE_URL_MAIN_NET = "https://api.violas.io"
    private const val VIOLAS_BASE_URL_TEST_NET = "https://api.violas.io"

    // Violas dex base url
    private const val VIOLAS_DEX_BASE_URL_MAIN_NET = "https://dex.violas.io"
    private const val VIOLAS_DEX_BASE_URL_TEST_NET = "https://dex.violas.io"

    // Libra base url
    private const val LIBRA_BASE_URL_MAIN_NET = "https://client.testnet.libra.org"
    private const val LIBRA_BASE_URL_TEST_NET = "https://client.testnet.libra.org"

    // Bitmain base url
    // 对外公开，有API速率限制（每分钟120次）
    private const val BITMAIN_OPEN_BASE_URL_MAIN_NET = "https://chain.api.btc.com/v3/"
    private const val BITMAIN_OPEN_BASE_URL_TEST_NET = "https://testnet-chain.api.btc.com/v3/"
    // 正式对接，无API速率限制
    private const val BITMAIN_BASE_URL_MAIN_NET =
        "https://developer-btc-chain.api.btc.com/appkey-e6e2ce95d8df/"
    private const val BITMAIN_BASE_URL_TEST_NET = "https://tchain.api.btc.com/v3/"

    // Libexplorer base url
    private const val LIBEXPLORER_BASE_URL_MAIN_NET = "https://api-test.libexplorer.com/api"
    private const val LIBEXPLORER_BASE_URL_TEST_NET = "https://api-test.libexplorer.com/api"

    // Trezor base url
    private const val TREZOR_BASE_URL_MAIN_NET = "https://tbtc1.trezor.io/api"
    private const val TREZOR_BASE_URL_TEST_NET = "https://tbtc1.trezor.io/api"

    fun getTrezorBaseUrl(): String {
        return if (Vm.TestNet)
            TREZOR_BASE_URL_TEST_NET
        else
            TREZOR_BASE_URL_MAIN_NET
    }

    fun getViolasBaseUrl(): String {
        return if (Vm.TestNet)
            VIOLAS_BASE_URL_TEST_NET
        else
            VIOLAS_BASE_URL_MAIN_NET
    }

    fun getViolasDexBaseUrl(): String {
        return if (Vm.TestNet)
            VIOLAS_DEX_BASE_URL_TEST_NET
        else
            VIOLAS_DEX_BASE_URL_MAIN_NET
    }

    fun getViolasDexSocketBaseUrl(): String {
        return if (Vm.TestNet)
            VIOLAS_DEX_BASE_URL_TEST_NET
        else
            VIOLAS_DEX_BASE_URL_MAIN_NET
    }

    fun getLibraBaseUrl(): String {
        return if (Vm.TestNet)
            LIBRA_BASE_URL_TEST_NET
        else
            LIBRA_BASE_URL_MAIN_NET
    }

    fun getBitmainOpenBaseUrl(): String {
        return if (Vm.TestNet)
            BITMAIN_OPEN_BASE_URL_TEST_NET
        else
            BITMAIN_OPEN_BASE_URL_MAIN_NET
    }

    fun getBitmainBaseUrl(): String {
        return if (Vm.TestNet)
            BITMAIN_BASE_URL_TEST_NET
        else
            BITMAIN_BASE_URL_MAIN_NET
    }

    fun getLibexplorerBaseUrl(): String {
        return if (Vm.TestNet)
            LIBEXPLORER_BASE_URL_TEST_NET
        else
            LIBEXPLORER_BASE_URL_MAIN_NET
    }
}
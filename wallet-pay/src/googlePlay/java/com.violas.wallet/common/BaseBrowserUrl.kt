package com.violas.wallet.common

object BaseBrowserUrl {
    // 对外公开，有API速率限制（每分钟120次）
    private const val BITMAIN_BASE_URL_MAIN_NET_OPEN = "https://chain.api.btc.com/v3/"
    private const val BITMAIN_BASE_URL_TEST_NET_OPEN = "https://testnet-chain.api.btc.com/v3/"
    // 正式对接，无API速率限制
    private const val BITMAIN_BASE_URL_MAIN_NET =
        "https://developer-btc-chain.api.btc.com/appkey-e6e2ce95d8df/"
    private const val BITMAIN_BASE_URL_TEST_NET = "https://tchain.api.btc.com/v3/"

    private const val LIBEXPLORER_BASE_URL_MAIN_NET = "https://api-test.libexplorer.com/api"
    private const val LIBEXPLORER_BASE_URL_TEST_NET = "https://api-test.libexplorer.com/api"

    private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_MAIN_NET =
        "https://live.blockcypher.com/btc"
    private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_TEST_NET =
        "https://live.blockcypher.com/btc-testnet"

    private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_MAIN_NET =
        "https://libexplorer.com"
    private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_TEST_NET =
        "https://libexplorer.com"

    private const val VIOLAS_BROWSER_BASE_URL_MAIN_NET =
        "https://testnet.violas.io"
    private const val VIOLAS_BROWSER_BASE_URL_TEST_NET =
        "https://testnet.violas.io"


    fun getBitmainOpenBaseUrl(): String {
        return if (Vm.TestNet)
            BITMAIN_BASE_URL_TEST_NET_OPEN
        else
            BITMAIN_BASE_URL_MAIN_NET_OPEN
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

    fun getBitcoinBrowserUrl(transactionHash: String): String {
        return "${if (Vm.TestNet)
            BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_TEST_NET
        else
            BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_MAIN_NET}/tx/$transactionHash"
    }

    fun getLibraBrowserUrl(version: String): String {
        return "${if (Vm.TestNet)
            LIBRA_BROWSER_BASE_URL_LIBEXPLORER_TEST_NET
        else
            LIBRA_BROWSER_BASE_URL_LIBEXPLORER_MAIN_NET}/version/$version"
    }

    fun getViolasBrowserUrl(version: String): String {
        return "${if (Vm.TestNet)
            VIOLAS_BROWSER_BASE_URL_TEST_NET
        else
            VIOLAS_BROWSER_BASE_URL_MAIN_NET}/app/Violas_version/$version"
    }
}
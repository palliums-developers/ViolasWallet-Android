package com.violas.wallet.common

object BaseBrowserUrl {

    private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_MAIN_NET =
        "https://live.blockcypher.com/btc"
    private const val BITCOIN_BROWSER_BASE_URL_BLOCKCYPHER_TEST_NET =
        "https://live.blockcypher.com/btc-testnet"

    private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_MAIN_NET =
        "https://libexplorer.com"
    private const val LIBRA_BROWSER_BASE_URL_LIBEXPLORER_TEST_NET =
        "https://libexplorer.com"

    private const val VIOLAS_BROWSER_BASE_URL_MAIN_NET =
        "http://47.52.66.26:10081"
    private const val VIOLAS_BROWSER_BASE_URL_TEST_NET =
        "http://47.52.66.26:10081"

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
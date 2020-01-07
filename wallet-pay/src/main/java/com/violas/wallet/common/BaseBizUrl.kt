package com.violas.wallet.common

object BaseBizUrl {
    // dex base url
    private const val DEX_BASE_URL_MAIN_NET = "http://18.220.66.235:38181"
    private const val DEX_BASE_URL_TEST_NET = "http://18.220.66.235:38181"

    // Violas base url
    private const val BASE_URL_MAIN_NET = "http://52.27.228.84:4000"
    private const val BASE_URL_TEST_NET = "http://52.27.228.84:4000"

    fun getDefaultBaseUrl(): String {
        return if (Vm.TestNet) BASE_URL_TEST_NET else BASE_URL_MAIN_NET
    }

    fun getDexBaseUrl(): String {
        return if (Vm.TestNet)
            DEX_BASE_URL_TEST_NET
        else
            DEX_BASE_URL_MAIN_NET
    }

    fun getDexSocketBaseUrl(): String {
        return if (Vm.TestNet)
            DEX_BASE_URL_TEST_NET
        else
            DEX_BASE_URL_MAIN_NET
    }
}
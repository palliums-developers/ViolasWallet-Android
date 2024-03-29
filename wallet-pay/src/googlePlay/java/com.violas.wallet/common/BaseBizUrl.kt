package com.violas.wallet.common

object BaseBizUrl {
    // dex base url
    private const val DEX_BASE_URL_MAIN_NET = "https://dex.violas.io"
    private const val DEX_BASE_URL_TEST_NET = "https://dex.violas.io"

    // Violas base url
    private const val BASE_URL_MAIN_NET = "https://api.violas.io"
    private const val BASE_URL_TEST_NET = "https://api.violas.io"

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
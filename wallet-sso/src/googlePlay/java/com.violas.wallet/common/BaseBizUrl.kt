package com.violas.wallet.common

object BaseBizUrl {
    // Violas base url
    private const val BASE_URL_MAIN_NET = "https://api.violas.io"
    private const val BASE_URL_TEST_NET = "https://api.violas.io"

    fun getDefaultBaseUrl(): String {
        return if (Vm.TestNet) BASE_URL_TEST_NET else BASE_URL_MAIN_NET
    }
}
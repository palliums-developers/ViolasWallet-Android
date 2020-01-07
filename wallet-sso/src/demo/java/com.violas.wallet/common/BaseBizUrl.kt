package com.violas.wallet.common

object BaseBizUrl {
    // Violas base url
    private const val BASE_URL_MAIN_NET = "http://52.27.228.84:4000"
    private const val BASE_URL_TEST_NET = "http://52.27.228.84:4000"

    fun getDefaultBaseUrl(): String {
        return if (Vm.TestNet) BASE_URL_TEST_NET else BASE_URL_MAIN_NET
    }
}
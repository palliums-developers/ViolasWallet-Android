package com.violas.wallet.common

/**
 * Created by elephant on 2/7/21 10:50 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getBitcoinTxnDetailsUrl(transactionHash: String): String {
    return "${WebBaseUrl.BLOCKCYPHER_EXPLORER_BASE_URL}/tx/$transactionHash"
}

fun getDiemTxnDetailsUrl(version: String): String {
    return "${WebBaseUrl.DIEMSCAN_EXPLORER_BASE_URL}/version/$version"
}

fun getViolasTxnDetailsUrl(version: String): String {
    return "${WebBaseUrl.VIOLAS_EXPLORER_BASE_URL}/version/$version"
}

fun getViolasFaucetUrl(address: String): String {
    return "${WebBaseUrl.VIOLAS_FAUCET_BASE_URL}/faucet/$address"
}

fun getViolasDappUrl(): String {
    return "${WebBaseUrl.VIOLAS_WEB_APP_BASE_URL}/violasMapping"
}

fun getViolasIncentiveHomeUrl(languageCode: String, address: String?): String {
    return "${WebBaseUrl.VIOLAS_WEB_APP_BASE_URL}/homepage/home/miningAwards?language=${
        languageCode
    }&address=${address ?: ""}"
}

fun getViolasIncentiveInviteUrl(languageCode: String, address: String?): String {
    return "${WebBaseUrl.VIOLAS_WEB_APP_BASE_URL}/homepage/home/inviteRewards?language=${
        languageCode
    }&address=${address ?: ""}"
}

fun getViolasIncentiveRulesUrl(languageCode: String, address: String?): String {
    return "${WebBaseUrl.VIOLAS_WEB_APP_BASE_URL}/homepage/home/ruleDescription?language=${
        languageCode
    }&address=${address ?: ""}"
}
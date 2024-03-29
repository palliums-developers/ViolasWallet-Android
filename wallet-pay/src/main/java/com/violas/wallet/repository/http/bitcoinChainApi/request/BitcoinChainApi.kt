package com.violas.wallet.repository.http.bitcoinChainApi.request


import com.violas.wallet.BuildConfig

object BitcoinChainApi {
    fun get(): BaseBitcoinChainRequest {
        //        switch (BuildConfig.TESTNET) {
        //            default:
        //            case "testnet":
        //            case "devnet":
        //                return new BTrusteeTestRequest();
        //            case "main":
        //                return new BTrusteeRequest();
        //        }
        var bitcoinChainVersionEnum = when (BuildConfig.TESTNET) {
            "testnet" -> BitcoinChainVersionEnum.TestNet
            "devnet" -> BitcoinChainVersionEnum.Dev
            "main" -> BitcoinChainVersionEnum.Main
            else -> BitcoinChainVersionEnum.TestNet
        }
        return BTCRequest(bitcoinChainVersionEnum)
    }
}

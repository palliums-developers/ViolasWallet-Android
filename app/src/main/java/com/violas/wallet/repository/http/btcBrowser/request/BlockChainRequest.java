package com.violas.wallet.repository.http.btcBrowser.request;


import com.violas.wallet.BuildConfig;

public class BlockChainRequest {
    public static BaseChainRequest get() {
        switch (BuildConfig.TESTNET) {
            default:
            case "testnet":
            case "devnet":
                return new BTrusteeTestRequest();
            case "main":
                return new BTrusteeRequest();
        }
//        switch (BuildConfig.TESTNET) {
//            default:
//            case "testnet":
//            case "devnet":
//                return new BTCTestRequest();
//            case "main":
//                return new BTCRequest();
//        }
    }
}

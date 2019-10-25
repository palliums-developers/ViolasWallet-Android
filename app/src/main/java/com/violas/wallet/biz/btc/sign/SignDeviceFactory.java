package com.violas.wallet.biz.btc.sign;

import android.util.Log;

import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

public class SignDeviceFactory {
    public static SignDevice get(UTXO utxo) {
        SignDevice signDevice;
        Log.e("====", "  =====  " + utxo.getUtxoType());
        int utxoType = utxo.getUtxoType();
        switch (utxoType) {
            case UTXO.P2PKH:
                signDevice = new P2PKHSignDevice(utxo);
                break;
            default:
            case UTXO.NONE:
            case UTXO.P2SH:
                signDevice = new P2SHSignDevice(utxo);
                break;
        }
        return signDevice;
//        throw new RuntimeException("");
    }
}

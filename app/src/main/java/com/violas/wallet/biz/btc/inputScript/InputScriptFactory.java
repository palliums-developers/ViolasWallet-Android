package com.violas.wallet.biz.btc.inputScript;


import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

public class InputScriptFactory {
    public static InputScript get(UTXO utxo) {
        switch (utxo.getUtxoType()) {
            case UTXO.P2PKH:
                return new P2PKHInputScript(utxo);
            case UTXO.P2SH:
                return new P2SHInputScript(utxo);
            case UTXO.NONE:
                return new P2PKHInputScript(utxo);
        }
        throw new RuntimeException("Not compatible with");
    }
}

package com.violas.wallet.biz.btc.inputScript;


import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.HexUtils;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

public class P2SHInputScript implements InputScript {

    //    @Override
//    public Script make(BitCoinECKeyPair bitCoinECKeyPair) {
//        return new Script(bitCoinECKeyPair.getRawAddress());
//    }
    private final UTXO mUTXO;

    public P2SHInputScript(UTXO utxo) {
        this.mUTXO = utxo;
    }

    @Override
    public Script make() {
        return new Script(HexUtils.fromHex(mUTXO.getScriptPubKey()));
    }
}

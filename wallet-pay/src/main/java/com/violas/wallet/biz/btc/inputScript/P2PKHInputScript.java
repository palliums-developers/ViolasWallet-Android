package com.violas.wallet.biz.btc.inputScript;

import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.HexUtils;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;

public class P2PKHInputScript implements InputScript {
    private final UTXO mUTXO;

    public P2PKHInputScript(UTXO utxo) {
        this.mUTXO = utxo;
    }

    @Override
    public Script make() {
        return new Script(HexUtils.fromHex(mUTXO.getScriptPubKey()));
    }
}

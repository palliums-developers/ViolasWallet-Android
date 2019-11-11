package com.violas.wallet.biz.btc.sign;

import com.quincysx.crypto.bitcoin.BTCTransaction;
import com.quincysx.crypto.bitcoin.script.Script;

public interface SignDevice {
    public Script sign(byte[] privateKey, byte[] publicKey, BTCTransaction btcTransaction);
}
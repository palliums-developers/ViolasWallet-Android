package com.violas.wallet.biz.btc.outputScript;

import com.quincysx.crypto.bitcoin.script.Script;

public interface OutputScript {
    Script make(byte[] bytes);
}


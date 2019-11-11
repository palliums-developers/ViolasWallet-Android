package com.violas.wallet.biz.btc.outputScript;

import com.quincysx.crypto.bitcoin.script.Script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class P2PKHOutputScript implements OutputScript {

    @Override
    public Script make(byte[] bytes) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream(25);
        buf.write(Script.OP_DUP);
        buf.write(Script.OP_HASH160);
        try {
            Script.writeBytes(bytes, buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf.write(Script.OP_EQUALVERIFY);
        buf.write(Script.OP_CHECKSIG);
        return new Script(buf.toByteArray());
    }
}

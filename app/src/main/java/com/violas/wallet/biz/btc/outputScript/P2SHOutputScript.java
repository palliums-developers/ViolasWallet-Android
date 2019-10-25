package com.violas.wallet.biz.btc.outputScript;

import com.quincysx.crypto.bitcoin.script.Script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class P2SHOutputScript implements OutputScript {

    @Override
    public Script make(byte[] bytes) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(Script.OP_HASH160);
        try {
            Script.writeBytes(bytes, buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf.write(Script.OP_EQUAL);
        return new Script(buf.toByteArray());
    }
}

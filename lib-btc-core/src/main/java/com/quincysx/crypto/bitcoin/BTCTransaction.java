/*
 The MIT License (MIT)

 Copyright (c) 2013 Valentin Konovalov

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.*/

package com.quincysx.crypto.bitcoin;

import com.quincysx.crypto.ECKeyPair;
import com.quincysx.crypto.Transaction;
import com.quincysx.crypto.bip32.ValidationException;
import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.BTCUtils;
import com.quincysx.crypto.utils.HexUtils;

import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class BTCTransaction implements Transaction {
    public final int version;
    public final Input[] inputs;
    public final Output[] outputs;
    public final int lockTime;

    public BTCTransaction(byte[] rawBytes) throws BitcoinException {
        if (rawBytes == null) {
            throw new BitcoinException(BitcoinException.ERR_NO_INPUT, "empty input");
        }
        BitcoinInputStream bais = null;
        try {
            bais = new BitcoinInputStream(rawBytes);
            version = bais.readInt32();
            if (version != 1 && version != 2 && version != 3) {
                throw new BitcoinException(BitcoinException.ERR_UNSUPPORTED, "Unsupported TX " +
                        "version", version);
            }
            int inputsCount = (int) bais.readVarInt();
            inputs = new Input[inputsCount];
            for (int i = 0; i < inputsCount; i++) {
                OutPoint outPoint = new OutPoint(BTCUtils.reverse(bais.readChars(32)), bais
                        .readInt32());
                byte[] script = bais.readChars((int) bais.readVarInt());
                int sequence = bais.readInt32();
                inputs[i] = new Input(outPoint, new Script(script), sequence);
            }
            int outputsCount = (int) bais.readVarInt();
            outputs = new Output[outputsCount];
            for (int i = 0; i < outputsCount; i++) {
                long value = bais.readInt64();
                long scriptSize = bais.readVarInt();
                if (scriptSize < 0 || scriptSize > 10_000_000) {
                    throw new BitcoinException(BitcoinException.ERR_BAD_FORMAT, "Script size for " +
                            "output " + i +
                            " is strange (" + scriptSize + " bytes).");
                }
                byte[] script = bais.readChars((int) scriptSize);
                outputs[i] = new Output(value, new Script(script));
            }
            lockTime = bais.readInt32();
//        } catch (EOFException e) {
//            throw new BitcoinException(BitcoinException.ERR_BAD_FORMAT, "TX incomplete");
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read TX");
        } catch (Error e) {
            throw new IllegalArgumentException("Unable to read TX: " + e);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public BTCTransaction(Input[] inputs, Output[] outputs, int lockTime) {
        this.version = 2;
        this.inputs = inputs;
        this.outputs = outputs;
        this.lockTime = lockTime;
    }

    @Override
    public byte[] getSignBytes() {
        return getBytes();
    }

    public byte[] getBytes() {
        BitcoinOutputStream baos = new BitcoinOutputStream();
        try {
            baos.writeInt32(version);
            baos.writeVarInt(inputs.length);
            for (Input input : inputs) {
                baos.write(BTCUtils.reverse(input.outPoint.hash));
                baos.writeInt32(input.outPoint.index);
                int scriptLen = input.script == null ? 0 : input.script.bytes.length;
                baos.writeVarInt(scriptLen);
                if (scriptLen > 0) {
                    baos.write(input.script.bytes);
                }
                baos.writeInt32(input.sequence);
            }
            baos.writeVarInt(outputs.length);
            for (Output output : outputs) {
                baos.writeInt64(output.value);
                int scriptLen = output.script == null ? 0 : output.script.bytes.length;
                baos.writeVarInt(scriptLen);
                if (scriptLen > 0) {
                    baos.write(output.script.bytes);
                }
            }
            baos.writeInt32(lockTime);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public String toString() {
        return "{" +
                "\n\"inputs\":\n" + printAsJsonArray(inputs) +
                ",\n\"outputs\":\n" + printAsJsonArray(outputs) +
                ",\n\"lockTime\":\"" + lockTime + "\"\n" +
                ",\n\"version\":\"" + version + "\"}\n";
    }

    private String printAsJsonArray(Object[] a) {
        if (a == null) {
            return "null";
        }
        if (a.length == 0) {
            return "[]";
        }
        int iMax = a.length - 1;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; ; i++) {
            sb.append(String.valueOf(a[i]));
            if (i == iMax)
                return sb.append(']').toString();
            sb.append(",\n");
        }
    }

    public static class Input {
        public final OutPoint outPoint;
        public Script script;
        public final int sequence;

        public Input(OutPoint outPoint, Script script, int sequence) {
            this.outPoint = outPoint;
            this.script = script;
            this.sequence = sequence;
        }

        @Override
        public String toString() {
            return "{\n\"outPoint\":" + outPoint + ",\n\"script\":\"" + script + "\"," +
                    "\n\"sequence\":\"" + Integer.toHexString(sequence) + "\"\n}\n";
        }
    }

    public static class OutPoint {
        public final byte[] hash;//32-byte hash of the transaction from which we want to redeem
        // an output
        public final int index;//Four-byte field denoting the output index we want to redeem from
        // the transaction with the above hash (output number 2 = output index 1)

        public OutPoint(byte[] hash, int index) {
            this.hash = hash;
            this.index = index;
        }

        @Override
        public String toString() {
            return "{" + "\"hash\":\"" + HexUtils.toHex(hash) + "\", \"index\":\"" + index + "\"}";
        }
    }

    public static class Output {
        public final long value;
        public final Script script;

        public Output(long value, Script script) {
            this.value = value;
            this.script = script;
        }

        @Override
        public String toString() {
            return "{\n\"value\":\"" + value * 1e-8 + "\",\"script\":\"" + script + "\"\n}";
        }
    }

    @Override
    public byte[] sign(ECKeyPair key) throws ValidationException {
        BTCTransaction sign = key.sign(this.getSignBytes());
        int length = sign.inputs.length;
        for (int i = 0; i < length; i++) {
            this.inputs[i] = sign.inputs[i];
        }
//        return sign.getSignBytes();
        return getSignBytes();
    }

}

package com.quincysx.crypto.bitcoin.script;

import com.quincysx.crypto.bip11.MultiSigAddress;
import com.quincysx.crypto.bitcoin.BTCTransaction;
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair;
import com.quincysx.crypto.bitcoin.BitcoinException;
import com.quincysx.crypto.bitcoin.BitcoinOutputStream;
import com.quincysx.crypto.utils.BTCUtils;
import com.quincysx.crypto.utils.Base58;
import com.quincysx.crypto.utils.HexUtils;
import com.quincysx.crypto.utils.RIPEMD160;
import com.quincysx.crypto.utils.SHA256;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Stack;

import static com.quincysx.crypto.utils.CheckUtils.checkArgument;

public final class Script {

    public static class ScriptInvalidException extends Exception {
        public ScriptInvalidException() {
        }

        public ScriptInvalidException(String s) {
            super(s);
        }
    }

    public static final byte OP_0 = 0;
    public static final byte OP_1 = 0x51;
    public static final byte OP_16 = 0x60;
    public static final byte OP_IF = 0x63;
    public static final byte OP_ELSE = 0x67;
    public static final byte OP_ENDIF = 0x68;
    public static final byte OP_1NEGATE = 0x4f;
    public static final byte OP_FALSE = 0;
    public static final byte OP_TRUE = 0x51;
    public static final byte OP_PUSHDATA1 = 0x4c;
    public static final byte OP_PUSHDATA2 = 0x4d;
    public static final byte OP_PUSHDATA4 = 0x4e;
    public static final byte OP_DUP = 0x76;//Duplicates the top stack item.
    public static final byte OP_DROP = 0x75;
    public static final byte OP_HASH160 = (byte) 0xA9;//The input is hashed twice: first with
    // SHA-256 and then with RIPEMD-160.
    public static final byte OP_VERIFY = 0x69;//Marks transaction as invalid if top stack
    // value is not true. True is removed, but false is not.
    public static final byte OP_EQUAL = (byte) 0x87;//Returns 1 if the inputs are exactly
    // equal, 0 otherwise.
    public static final byte OP_EQUALVERIFY = (byte) 0x88;//Same as OP_EQUAL, but runs
    // OP_VERIFY afterward.
    public static final byte OP_CHECKSIG = (byte) 0xAC;//The entire transaction's outputs,
    // inputs, and script (from the most recently-executed OP_CODESEPARATOR to the end) are
    // hashed. The signature used by OP_CHECKSIG must be a valid signature for this hash and
    // public key. If it is, 1 is returned, 0 otherwise.
    public static final byte OP_CHECKSIGVERIFY = (byte) 0xAD;
    public static final byte OP_NOP = 0x61;

    public static final byte OP_RETURN = 0x6a;
    public static final byte OP_CHECKMULTISIG = (byte) 0xae;
    public static final byte OP_CHECKSEQUENCEVERIFY = (byte) 0xb2;
    public static final byte OP_CHECKMULTISIGVERIFY = (byte) 0xaf;

    public static final byte SIGHASH_ALL = 1;

    public final byte[] bytes;

    public Script(byte[] rawBytes) {
        bytes = rawBytes;
    }

    public Script(byte[] data1, byte[] data2) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data1.length + data2.length + 2);
        try {
            writeBytes(data1, baos);
            writeBytes(data2, baos);
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bytes = baos.toByteArray();
    }

    public static void writeBytes(byte[] data, ByteArrayOutputStream baos) throws IOException {
        if (data.length < OP_PUSHDATA1) {
            baos.write(data.length);
        } else if (data.length < 0xff) {
            baos.write(OP_PUSHDATA1);
            baos.write(data.length);
        } else if (data.length < 0xffff) {
            baos.write(OP_PUSHDATA2);
            baos.write(data.length & 0xff);
            baos.write((data.length >> 8) & 0xff);
        } else {
            baos.write(OP_PUSHDATA4);
            baos.write(data.length & 0xff);
            baos.write((data.length >> 8) & 0xff);
            baos.write((data.length >> 16) & 0xff);
            baos.write((data.length >>> 24) & 0xff);
        }
        baos.write(data);
    }

    public void run(Stack<byte[]> stack) throws ScriptInvalidException {
        run(0, null, stack);
    }

    public void run(int inputIndex, BTCTransaction tx, Stack<byte[]> stack) throws
            ScriptInvalidException {
        for (int pos = 0; pos < bytes.length; pos++) {
            switch (bytes[pos]) {
                case OP_NOP:
                    break;
                case OP_DROP:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("stack empty on OP_DROP");
                    }
                    stack.pop();
                    break;
                case OP_DUP:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("stack empty on OP_DUP");
                    }
                    stack.push(stack.peek());
                    break;
                case OP_HASH160:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("stack empty on OP_HASH160");
                    }
                    stack.push(RIPEMD160.hash160(stack.pop()));
                    break;
                case OP_EQUAL:
                case OP_EQUALVERIFY:
                    if (stack.size() < 2) {
                        throw new IllegalArgumentException("not enough elements to perform " +
                                "OP_EQUAL");
                    }
                    stack.push(new byte[]{(byte) (Arrays.equals(stack.pop(), stack.pop()) ? 1
                            : 0)});
                    if (bytes[pos] == OP_EQUALVERIFY) {
                        if (verifyFails(stack)) {
                            throw new ScriptInvalidException("wrong address");
                        }
                    }
                    break;
                case OP_VERIFY:
                    if (verifyFails(stack)) {
                        throw new ScriptInvalidException();
                    }
                    break;
                case OP_CHECKSIG:
                case OP_CHECKSIGVERIFY:
                    byte[] publicKey = stack.pop();
                    byte[] signatureAndHashType = stack.pop();
                    if (signatureAndHashType[signatureAndHashType.length - 1] != SIGHASH_ALL) {
                        throw new IllegalArgumentException("I cannot check this sig type: " +
                                signatureAndHashType[signatureAndHashType.length - 1]);
                    }
                    byte[] signature = new byte[signatureAndHashType.length - 1];
                    System.arraycopy(signatureAndHashType, 0, signature, 0, signature.length);
                    byte[] hash = hashTransaction(inputIndex, bytes, tx);
                    boolean valid = BTCUtils.verify(publicKey, signature, hash);
                    if (bytes[pos] == OP_CHECKSIG) {
                        stack.push(new byte[]{(byte) (valid ? 1 : 0)});
                    } else {
                        if (verifyFails(stack)) {
                            throw new ScriptInvalidException("Bad signature");
                        }
                        if (!stack.empty()) {
                            throw new ScriptInvalidException("Bad signature - superfluous " +
                                    "scriptSig operations");
                        }
                    }
                    break;
                case OP_FALSE:
                    stack.push(new byte[]{0});
                    break;
                case OP_TRUE:
                    stack.push(new byte[]{1});
                    break;
                default:
                    int op = bytes[pos] & 0xff;
                    int len;
                    if (op < OP_PUSHDATA1) {
                        len = op;
                        byte[] data = new byte[len];
                        System.arraycopy(bytes, pos + 1, data, 0, len);
                        stack.push(data);
                        pos += data.length;
                    } else if (op == OP_PUSHDATA1) {
                        len = bytes[pos + 1] & 0xff;
                        byte[] data = new byte[len];
                        System.arraycopy(bytes, pos + 1, data, 0, len);
                        stack.push(data);
                        pos += 1 + data.length;
                    } else {
                        throw new IllegalArgumentException("I cannot read this data: " +
                                Integer.toHexString(bytes[pos]));
                    }
                    break;
            }
        }
    }

    public static byte[] hashTransaction(int inputIndex, byte[] subscript, BTCTransaction tx) {
        BTCTransaction.Input[] unsignedInputs = new BTCTransaction.Input[tx.inputs.length];
        for (int i = 0; i < tx.inputs.length; i++) {
            BTCTransaction.Input txInput = tx.inputs[i];
            if (i == inputIndex) {
                unsignedInputs[i] = new BTCTransaction.Input(txInput.outPoint, new Script(subscript),
                        txInput.sequence);
            } else {
                unsignedInputs[i] = new BTCTransaction.Input(txInput.outPoint, new Script(new byte[0]),
                        txInput.sequence);
            }
        }
        BTCTransaction unsignedTransaction = new BTCTransaction(unsignedInputs, tx.outputs,
                tx.lockTime);
        return hashTransactionForSigning(unsignedTransaction);
    }

    public static byte[] hashTransactionForSigning(BTCTransaction unsignedTransaction) {
        byte[] txUnsignedBytes = unsignedTransaction.getSignBytes();
        BitcoinOutputStream baos = new BitcoinOutputStream();
        try {
            baos.write(txUnsignedBytes);
            baos.writeInt32(Script.SIGHASH_ALL);
            baos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return SHA256.doubleSha256(baos.toByteArray());
    }

    public static boolean verifyFails(Stack<byte[]> stack) {
        byte[] input;
        boolean valid;
        input = stack.pop();
        if (input.length == 0 || (input.length == 1 && input[0] == OP_FALSE)) {
            //false
            stack.push(new byte[]{OP_FALSE});
            valid = false;
        } else {
            //true
            valid = true;
        }
        return !valid;
    }


    @Override
    public String toString() {
        return convertBytesToReadableString(bytes);
    }

    //converts something like "OP_DUP OP_HASH160 ba507bae8f1643d2556000ca26b9301b9069dc6b
    // OP_EQUALVERIFY OP_CHECKSIG" into bytes
    public static byte[] convertReadableStringToBytes(String readableString) {
        String[] tokens = readableString.trim().split("\\s+");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (String token : tokens) {
            switch (token) {
                case "OP_NOP":
                    os.write(OP_NOP);
                    break;
                case "OP_DROP":
                    os.write(OP_DROP);
                    break;
                case "OP_DUP":
                    os.write(OP_DUP);
                    break;
                case "OP_HASH160":
                    os.write(OP_HASH160);
                    break;
                case "OP_EQUAL":
                    os.write(OP_EQUAL);
                    break;
                case "OP_EQUALVERIFY":
                    os.write(OP_EQUALVERIFY);
                    break;
                case "OP_VERIFY":
                    os.write(OP_VERIFY);
                    break;
                case "OP_CHECKSIG":
                    os.write(OP_CHECKSIG);
                    break;
                case "OP_CHECKSIGVERIFY":
                    os.write(OP_CHECKSIGVERIFY);
                    break;
                case "OP_FALSE":
                    os.write(OP_FALSE);
                    break;
                case "OP_TRUE":
                    os.write(OP_TRUE);
                    break;
                case "OP_CHECKMULTISIGVERIFY":
                    os.write(OP_CHECKMULTISIGVERIFY);
                    break;
                case "OP_CHECKSEQUENCEVERIFY":
                    os.write(OP_CHECKSEQUENCEVERIFY);
                    break;
                default:
                    if (token.startsWith("OP_")) {
                        throw new IllegalArgumentException("I don't know this operation: " +
                                token);
                    }
                    byte[] data = HexUtils.fromHex(token);
                    if (data == null) {
                        throw new IllegalArgumentException("I don't know what's this: " +
                                token);
                    }
                    if (data.length < OP_PUSHDATA1) {
                        os.write(data.length);
                        try {
                            os.write(data);
                        } catch (IOException e) {
                            throw new RuntimeException("ByteArrayOutputStream behaves weird: " +
                                    "" + e);
                        }
                    } else if (data.length <= 255) {
                        os.write(OP_PUSHDATA1);
                        os.write(data.length);
                        try {
                            os.write(data);
                        } catch (IOException e) {
                            throw new RuntimeException("ByteArrayOutputStream behaves weird: " +
                                    "" + e);
                        }
                    } else {
                        throw new IllegalArgumentException("OP_PUSHDATA2 & OP_PUSHDATA4 are " +
                                "not supported");
                    }
                    break;
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return os.toByteArray();
    }

    public static String convertBytesToReadableString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < bytes.length; pos++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (bytes[pos] >= OP_1 && bytes[pos] <= OP_16) {
                sb.append("OP_" + (bytes[pos] - OP_1 + 1));
                continue;
            }

            switch (bytes[pos]) {
                case OP_1NEGATE:
                    sb.append("OP_1NEGATE");
                case OP_IF:
                    sb.append("OP_IF");
                case OP_ELSE:
                    sb.append("OP_ELSE");
                case OP_ENDIF:
                    sb.append("OP_ENDIF");
                case OP_CHECKMULTISIGVERIFY:
                    sb.append("OP_CHECKMULTISIGVERIFY");
                case OP_RETURN:
                    sb.append("OP_RETURN");
                case OP_CHECKSEQUENCEVERIFY:
                    sb.append("OP_CHECKSEQUENCEVERIFY");
                case OP_CHECKMULTISIG:
                    sb.append("OP_CHECKMULTISIG");
                case OP_NOP:
                    sb.append("OP_NOP");
                    break;
                case OP_DROP:
                    sb.append("OP_DROP");
                    break;
                case OP_DUP:
                    sb.append("OP_DUP");
                    break;
                case OP_HASH160:
                    sb.append("OP_HASH160");
                    break;
                case OP_EQUAL:
                    sb.append("OP_EQUAL");
                    break;
                case OP_EQUALVERIFY:
                    sb.append("OP_EQUALVERIFY");
                    break;
                case OP_VERIFY:
                    sb.append("OP_VERIFY");
                    break;
                case OP_CHECKSIG:
                    sb.append("OP_CHECKSIG");
                    break;
                case OP_CHECKSIGVERIFY:
                    sb.append("OP_CHECKSIGVERIFY");
                    break;
                case OP_FALSE:
                    sb.append("OP_FALSE");
                    break;
                case OP_TRUE:
                    sb.append("OP_TRUE");
                    break;
                default:
                    int op = bytes[pos] & 0xff;
                    int len;
                    if (op < OP_PUSHDATA1) {
                        len = op;
                        byte[] data = new byte[len];
                        System.arraycopy(bytes, pos + 1, data, 0, len);
                        sb.append(HexUtils.toHex(data));
                        pos += data.length;
                    } else if (op == OP_PUSHDATA1) {
                        len = bytes[pos + 1] & 0xff;
                        byte[] data = new byte[len];
                        System.arraycopy(bytes, pos + 1, data, 0, len);//FIXME I suspect
                        // there is off by one error...
                        sb.append(HexUtils.toHex(data));
                        pos += 1 + data.length;
                    } else {
                        throw new IllegalArgumentException("I cannot read this data: " +
                                Integer.toHexString(bytes[pos]) + " at " + pos);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && Arrays.equals
                (bytes, ((Script) o).bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    public static Script buildOutput(String address) throws BitcoinException {
        //noinspection TryWithIdenticalCatches
        try {
            byte[] addressWithCheckSumAndNetworkCode = Base58.decode(address);

            if (addressWithCheckSumAndNetworkCode[0] != BitCoinECKeyPair.TEST_NET_ADDRESS_SUFFIX
                    && addressWithCheckSumAndNetworkCode[0] != BitCoinECKeyPair.MAIN_NET_ADDRESS_SUFFIX
                    && addressWithCheckSumAndNetworkCode[0] != MultiSigAddress.TEST_P2SH_ADDRESS_PREFIX
                    && addressWithCheckSumAndNetworkCode[0] != MultiSigAddress.MAIN_P2SH_ADDRESS_PREFIX) {
                throw new BitcoinException(BitcoinException.ERR_UNSUPPORTED, "Unknown address" +
                        " type", address);
            }
            byte[] bareAddress = new byte[20];
            System.arraycopy(addressWithCheckSumAndNetworkCode, 1, bareAddress, 0,
                    bareAddress.length);
            MessageDigest digestSha = MessageDigest.getInstance("SHA-256");
            digestSha.update(addressWithCheckSumAndNetworkCode, 0,
                    addressWithCheckSumAndNetworkCode.length - 4);
            byte[] calculatedDigest = digestSha.digest(digestSha.digest());
            for (int i = 0; i < 4; i++) {
                if (calculatedDigest[i] !=
                        addressWithCheckSumAndNetworkCode[addressWithCheckSumAndNetworkCode
                                .length - 4 + i]) {
                    throw new BitcoinException(BitcoinException.ERR_BAD_FORMAT, "Bad " +
                            "address", address);
                }
            }

            if (addressWithCheckSumAndNetworkCode[0] == MultiSigAddress.TEST_P2SH_ADDRESS_PREFIX
                    || addressWithCheckSumAndNetworkCode[0] == MultiSigAddress.MAIN_P2SH_ADDRESS_PREFIX) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                buf.write(OP_HASH160);
                writeBytes(bareAddress, buf);
                buf.write(OP_EQUAL);
                return new Script(buf.toByteArray());
            } else {
                ByteArrayOutputStream buf = new ByteArrayOutputStream(25);
                buf.write(OP_DUP);
                buf.write(OP_HASH160);
                writeBytes(bareAddress, buf);
                buf.write(OP_EQUALVERIFY);
                buf.write(OP_CHECKSIG);
                return new Script(buf.toByteArray());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int decodeFromOpN(int opcode) {
        checkArgument((opcode == OP_0 || opcode == OP_1NEGATE) || (opcode >= OP_1 && opcode <= OP_16),
                "decodeFromOpN called on non OP_N opcode: %s", opcode);
        if (opcode == OP_0)
            return 0;
        else if (opcode == OP_1NEGATE)
            return -1;
        else
            return opcode + 1 - OP_1;
    }

    public static int encodeToOpN(int value) {
        checkArgument(value >= -1 && value <= 16, "encodeToOpN called for " + value + " which we cannot encode in an opcode.");
        if (value == 0)
            return OP_0;
        else if (value == -1)
            return OP_1NEGATE;
        else
            return value - 1 + OP_1;
    }
}

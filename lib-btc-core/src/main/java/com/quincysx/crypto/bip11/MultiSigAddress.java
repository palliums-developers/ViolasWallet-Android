package com.quincysx.crypto.bip11;

import android.util.Log;

import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.Base58;
import com.quincysx.crypto.utils.HexUtils;
import com.quincysx.crypto.utils.RIPEMD160;
import com.quincysx.crypto.utils.SHA256;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static com.quincysx.crypto.utils.CheckUtils.checkArgument;

public class MultiSigAddress {
    public static final byte TEST_P2SH_ADDRESS_PREFIX = (byte) 0xC4;
    public static final byte MAIN_P2SH_ADDRESS_PREFIX = (byte) 0x5;

    public static byte[] generateMultiSigAddress(int threshold, byte[]... rawPubKeys) {
        checkArgument(threshold > 0);
        checkArgument(threshold <= rawPubKeys.length);
        checkArgument(rawPubKeys.length <= 16);  // That's the max we can represent with a single opcode.
        if (rawPubKeys.length > 3) {
            Log.w("MultiSigAddress", "Creating a multi-signature output that is non-standard: {} pubkeys, should be <= 3 " + rawPubKeys.length);
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            buf.write(Script.encodeToOpN(threshold));
            for (byte[] publicKey : rawPubKeys) {
                Script.writeBytes(publicKey, buf);
            }
            buf.write(Script.encodeToOpN(rawPubKeys.length));
            buf.write(Script.OP_CHECKMULTISIG);
            return buf.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buf.reset();
            try {
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new byte[]{};
    }

    public static String formatMultiSigAddress(boolean testnet, byte[] multiSigAddress) {
        byte[] hash160Address = RIPEMD160.hash160(multiSigAddress);
        byte[] addressBytes = new byte[1 + hash160Address.length + 4];
        //拼接测试网络或正式网络前缀
        addressBytes[0] = (byte) (testnet ? TEST_P2SH_ADDRESS_PREFIX : MAIN_P2SH_ADDRESS_PREFIX);

        System.arraycopy(hash160Address, 0, addressBytes, 1, hash160Address.length);
        //进行双 Sha256 运算
        byte[] check = SHA256.doubleSha256(addressBytes, 0, addressBytes.length - 4);

        //将双 Sha256 运算的结果前 4位 拼接到尾部
        System.arraycopy(check, 0, addressBytes, hash160Address.length + 1, 4);

        Log.e("=====","addressBytes "+HexUtils.toHex(hash160Address));

        Arrays.fill(hash160Address, (byte) 0);
        Arrays.fill(check, (byte) 0);
        return Base58.encode(addressBytes);
    }

    public static String generateMultiSigAddressToFormat(boolean testnet, int threshold, byte[]... rawPubKeys) {
        byte[] address = generateMultiSigAddress(threshold, rawPubKeys);
        Log.e("======", HexUtils.toHex(address));
        return formatMultiSigAddress(testnet, address);
    }
}

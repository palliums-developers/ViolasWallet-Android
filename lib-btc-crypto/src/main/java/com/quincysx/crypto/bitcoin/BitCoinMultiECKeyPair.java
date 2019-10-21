package com.quincysx.crypto.bitcoin;

import com.quincysx.crypto.bip11.MultiSigAddress;
import com.quincysx.crypto.bip32.ValidationException;
import com.quincysx.crypto.bitcoin.script.Script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author QuincySx
 * @date 2018/3/8 上午10:56
 */
public class BitCoinMultiECKeyPair extends BitCoinECKeyPair {
    private final Map<String, BitCoinECKeyPair> mECKeyPairMap = new LinkedHashMap<>();
    private String mAddress;
    private byte[] mMultiAddressScript = new byte[0];

    public BitCoinMultiECKeyPair(byte[] MultiAddressScript, List<BitCoinECKeyPair> keyPairs) throws ValidationException {
        this(MultiSigAddress.formatMultiSigAddress(keyPairs.get(0).testNet, MultiAddressScript), keyPairs);
        mMultiAddressScript = MultiAddressScript;
    }

    public BitCoinMultiECKeyPair(byte[] MultiAddressScript, BitCoinECKeyPair... keyPairs) throws ValidationException {
        this(MultiSigAddress.formatMultiSigAddress(keyPairs[0].testNet, MultiAddressScript), keyPairs);
        mMultiAddressScript = MultiAddressScript;
    }

    private BitCoinMultiECKeyPair(String address, List<BitCoinECKeyPair> keyPairs) throws ValidationException {
        super(new byte[32], keyPairs.get(0).testNet, true);
        for (BitCoinECKeyPair bitCoinECKeyPair : keyPairs) {
            mECKeyPairMap.put(bitCoinECKeyPair.getAddress(), bitCoinECKeyPair);
        }
        mAddress = address;
    }

    private BitCoinMultiECKeyPair(String address, BitCoinECKeyPair... keyPairs) throws ValidationException {
        this(address, new ArrayList<>(Arrays.asList(keyPairs)));
    }

    @Override
    public byte[] sign(BTCTransaction btcTransaction) {
        int count = mMultiAddressScript[0] - Script.OP_1;
        int i = 0;

        BitcoinOutputStream stream = new BitcoinOutputStream();
        stream.write(Script.encodeToOpN(0));
        for (BitCoinECKeyPair keyPair : mECKeyPairMap.values()) {
            if (i > count) {
                continue;
            }
            i++;
            try {
                Script.writeBytes(keyPair.sign(btcTransaction), stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Script.writeBytes(mMultiAddressScript, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public byte[] getRawAddress() {
        return mMultiAddressScript;
    }
}

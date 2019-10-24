package com.violas.wallet.biz.btc.outputScript;

import com.quincysx.crypto.bip11.MultiSigAddress;
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair;
import com.quincysx.crypto.bitcoin.BitcoinException;
import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.Base58;

import java.security.MessageDigest;

public class OutputScriptFactory {
    public static Script  buildOutput(String address) throws BitcoinException {
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

            if (addressWithCheckSumAndNetworkCode[0] == BitCoinECKeyPair.TEST_NET_ADDRESS_SUFFIX
                    || addressWithCheckSumAndNetworkCode[0] == BitCoinECKeyPair.MAIN_NET_ADDRESS_SUFFIX) {
                return new P2PKHOutputScript().make(bareAddress);
            } else {
                return new P2SHOutputScript().make(bareAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

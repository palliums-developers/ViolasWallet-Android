package com.violas.wallet.biz.btc.sign;

import com.quincysx.crypto.bitcoin.BTCTransaction;
import com.quincysx.crypto.bitcoin.BitCoinECKeyPair;
import com.quincysx.crypto.bitcoin.script.Script;
import com.violas.wallet.common.Vm;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;

public class P2PKHSignDevice implements SignDevice {
    private final UTXO mUTXO;

//    private AccountAddressDao mAccountAddressDao = WalletApplication.getInstances().getDatabase().accountAddressDao();

    public P2PKHSignDevice(UTXO utxo) {
        this.mUTXO = utxo;
    }

    @Override
    public Script sign(byte[] privateKey, byte[] publicKey, BTCTransaction btcTransaction) {
        try {
//            AccountAddressEntity keyEntity = mAccountAddressDao.findByAddress(mUTXO.getAddress());

            BitCoinECKeyPair bitCoinECKeyPair = new BitCoinECKeyPair(
                    privateKey,
                    publicKey,
                    Vm.TestNet
            );
            byte[] sign = bitCoinECKeyPair.sign(btcTransaction);
            return new Script(sign, bitCoinECKeyPair.getRawPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            return new Script(null);
        }
    }

}

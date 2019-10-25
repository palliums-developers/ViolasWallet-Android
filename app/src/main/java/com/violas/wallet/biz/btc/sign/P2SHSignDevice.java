package com.violas.wallet.biz.btc.sign;


import com.quincysx.crypto.bitcoin.BTCTransaction;
import com.quincysx.crypto.bitcoin.script.Script;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

public class P2SHSignDevice implements SignDevice {
    private final UTXO mUTXO;
//    private AccountAddressDao mAccountAddressDao = WalletApplication.getInstances().getDatabase().accountAddressDao();

    public P2SHSignDevice(UTXO utxo) {
        this.mUTXO = utxo;
    }

    @Override
    public Script sign(byte[] privateKey, byte[] publicKey, BTCTransaction btcTransaction) {
//        DepositUTXO utxo = (DepositUTXO) mUTXO;
//        String scriptAddress = utxo.getEntity().getScriptAddress();
//        List<String> scriptBeans = new Gson().fromJson(scriptAddress, new TypeToken<List<String>>() {
//        }.getType());
//
//        byte[] mMultiAddressScript = HexUtils.fromHex(utxo.getEntity().getScript());
//
//        int count = mMultiAddressScript[0] - Script.OP_1;
//        int i = 0;
//
//        BitcoinOutputStream stream = new BitcoinOutputStream();
//        stream.write(Script.encodeToOpN(0));
//        for (String keyPair : scriptBeans) {
//            if (i > count) {
//                continue;
//            }
//            i++;
//            try {
//                AccountAddressEntity keyEntity = mAccountAddressDao.findByAddress(keyPair);
//                BitCoinECKeyPair bitCoinECKeyPair = new BitCoinECKeyPair(SecureStorageHandler.decrypt(keyEntity.getKeyPrivate()), keyEntity.getKeyPublic(), Vm.TestNet);
//                Script.writeBytes(bitCoinECKeyPair.sign(btcTransaction), stream);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            Script.writeBytes(mMultiAddressScript, stream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return new Script(stream.toByteArray());

        return new Script(null);
    }

}

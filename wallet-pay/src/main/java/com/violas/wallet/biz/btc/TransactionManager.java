package com.violas.wallet.biz.btc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.palliums.content.ContextProvider;
import com.quincysx.crypto.Transaction;
import com.quincysx.crypto.bitcoin.BTCTransaction;
import com.quincysx.crypto.bitcoin.BitcoinException;
import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.HexUtils;
import com.violas.wallet.R;
import com.violas.wallet.biz.btc.inputScript.InputScriptFactory;
import com.violas.wallet.biz.btc.outputScript.OutputScriptFactory;
import com.violas.wallet.biz.btc.sign.SignDeviceFactory;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class TransactionManager {
    public static final int DEF_TO_ADDRESS_COUNT = 2;
    public static final int DEF_PROGRESS = 60;

    private final FeeManager mFeeManager;
    private final UTXOListManager mUTXOListManager;
    private double mAmount;
    private String mToAddress;
    private int mProgress = DEF_PROGRESS;
    private int mToAddressCount = DEF_TO_ADDRESS_COUNT;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private FeeCallback mFeeCallback;

    public TransactionManager(List<String> localEcKey, LinkedHashMap<String, UTXO> useUTXOList) {
        mUTXOListManager = new UTXOListManager(localEcKey, useUTXOList);
        mFeeManager = new FeeManager();
    }

    public TransactionManager(List<String> localEcKey) {
        mUTXOListManager = new UTXOListManager(localEcKey);
        mFeeManager = new FeeManager();
    }

    public UTXOListManager getUTXOListManager() {
        return mUTXOListManager;
    }

    public void setFeeCallback(FeeCallback callback) {
        mFeeCallback = callback;
    }

    public void transferIntent(double amount, String toAddress) {
        transferIntent(amount, toAddress, 60);
    }

    public void transferAmountIntent(double amount) {
        transferAmountIntent(amount, 60);
    }

    public void transferProgressIntent(int progress) {
        transferAmountIntent(mAmount, progress);
    }

    public void transferAmountIntent(double amount, int progress) {
        mAmount = amount;
        mProgress = progress;
        obtainUTXO(amount, mToAddressCount, progress);
    }

    public void transferIntent(double amount, String toAddress, int progress) {
        mAmount = amount;
        mToAddress = toAddress;
        mProgress = progress;
        obtainUTXO(amount, mToAddressCount, progress);
    }

    private Boolean obtainUTXO() {
        return obtainUTXO(mAmount, mToAddressCount, mProgress);
    }

    private Boolean obtainUTXO(double amount, int toAddressCount, int progress) {
        mAmount = amount;
        mProgress = progress;
        mToAddressCount = toAddressCount;

        mUTXOListManager.togetherUTXO(amount);

        int cursor;
        BigDecimal fee;
        do {
            cursor = mUTXOListManager.getAddressCursor();

            fee = mFeeManager.calculateFee(mUTXOListManager.getUTXOList(), toAddressCount, progress);

            mUTXOListManager.togetherUTXO(new BigDecimal(amount + "").add(fee).doubleValue());
        } while (cursor != mUTXOListManager.getAddressCursor());

        if (mFeeCallback != null) {
            mFeeCallback.onFees(fee.stripTrailingZeros().toPlainString());
        }
        return mUTXOListManager.checkBalance();
    }

    /**
     * 构建交易
     *
     * @param amount
     * @param toAddressCount
     * @param progress
     * @return
     */
    public Observable<Boolean> checkBalance(double amount, int toAddressCount, int progress) {
        return Observable
                .create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                        Boolean obtainUTXO = obtainUTXO(amount, toAddressCount, progress);
                        emitter.onNext(obtainUTXO);
                        emitter.onComplete();
                    }
                });
    }

    public boolean checkBalance(double amount, int toAddressCount) {
        return obtainUTXO(amount, toAddressCount, 35);
    }

    /**
     * 构建交易
     *
     * @param charge        金额是否足够
     * @param toAddress     目标地址
     * @param changeAddress 找零地址
     * @param changeAddress 找零地址
     * @param omniScript    omni 脚本
     */
    public Observable<Transaction> obtainTransaction(byte[] privateKey, byte[] publicKey, boolean charge, String toAddress, String changeAddress, Script omniScript) {
        mToAddress = toAddress;

        List<Pair<String, Long>> toAddressList = new ArrayList<>();

        return Observable.create(new ObservableOnSubscribe<Transaction>() {
            @Override
            public void subscribe(ObservableEmitter<Transaction> emitter) throws Exception {
                int outputCount = mToAddressCount;
                if (omniScript != null) {
                    outputCount = mToAddressCount + 1;
                }
                BigDecimal fee = mFeeManager.calculateFee(mUTXOListManager.getUTXOList(), outputCount, mProgress);
                BigDecimal subtract = mUTXOListManager.getUseAmount().subtract(fee).subtract(new BigDecimal(mAmount + ""));

                toAddressList.add(new Pair<>(changeAddress, subtract.multiply(new BigDecimal("100000000")).longValue()));

                toAddressList.add(new Pair<>(mToAddress, new BigDecimal(mAmount + "").multiply(new BigDecimal("100000000")).longValue()));
                
                Log.e("====", "到没到目标金额 " + charge + "   总计余额：" + mUTXOListManager.getUseAmount().doubleValue());

                if (!charge) {
                    emitter.onError(new Exception(ContextProvider.INSTANCE.getContext().getResources().getString(R.string.hint_insufficient_or_trading_fees_are_confirmed)));
                    emitter.onComplete();
                } else {
                    emitter.onNext(TransactionManager.generateTransaction(privateKey, publicKey, mUTXOListManager, toAddressList, omniScript));
                }
            }
        });
    }

    /**
     * 构建交易
     *
     * @param charge        金额是否足够
     * @param toAddress     目标地址
     * @param changeAddress 找零地址
     * @return
     */
    public Observable<Transaction> obtainTransaction(byte[] privateKey, byte[] publicKey, boolean charge, String toAddress, String changeAddress) {
        return obtainTransaction(privateKey, publicKey, charge, toAddress, changeAddress, null);
    }

    public static Transaction sign(byte[] privateKey, byte[] publicKey, BTCTransaction btcTransaction, UTXOListManager utxoListManager) {
        int inputCount = btcTransaction.inputs.length;
        Script[] scripts = new Script[inputCount];
        for (int i = 0; i < inputCount; i++) {
            clearInputScript(btcTransaction);
            BTCTransaction.Input input = btcTransaction.inputs[i];

            UTXO utxo = utxoListManager.getUTXOById(HexUtils.toHex(input.outPoint.hash), input.outPoint.index);

//            String publicScript = mkPubKeyScript(privateKey.getAddress());
//
//            Log.e("=====", "lod Script " + utxo.getScriptPubKey());
//            Log.e("=====", "lod Script " + HexUtils.toHex(new Script(HexUtils.fromHex(publicScript)).bytes));

            input.script = InputScriptFactory.get(utxo).make();

            scripts[i] = SignDeviceFactory.get(utxo).sign(privateKey, publicKey, btcTransaction);
        }

        for (int i = 0; i < inputCount; i++) {
            btcTransaction.inputs[i].script = scripts[i];
        }
        return btcTransaction;
    }

    private static void clearInputScript(BTCTransaction btcTransaction) {
        for (BTCTransaction.Input input : btcTransaction.inputs) {
            input.script = null;
        }
    }

    public static Transaction generateTransaction(byte[] privateKey, byte[] publicKey, UTXOListManager utxoListManager, List<Pair<String, Long>> toAddressList, Script omniScript) throws BitcoinException {
        List<UTXO> utxoList = utxoListManager.getUTXOList();
        BTCTransaction.Input[] inputs = generateTransactionInput(utxoList);
        BTCTransaction.Output[] outputs = generateTransactionOutput(toAddressList, omniScript);

        BTCTransaction btcTransaction = new BTCTransaction(inputs, outputs, 0);
        Transaction sign = TransactionManager.sign(privateKey, publicKey, btcTransaction, utxoListManager);
        Log.e("====", "sign " + HexUtils.toHex(sign.getSignBytes()));
        return sign;
    }

    public static BTCTransaction.Output[] generateTransactionOutput(List<Pair<String, Long>> toAddress, @Nullable Script omniScript) throws BitcoinException {
        int outputCount = toAddress.size();
        if (omniScript != null) {
            outputCount += 1;
        }
        List<BTCTransaction.Output> outputs = new ArrayList<>(outputCount);

        for (Pair<String, Long> address : toAddress) {
            BTCTransaction.Output input = new BTCTransaction.Output(
                    address.second,
                    OutputScriptFactory.buildOutput(address.first)
            );
            outputs.add(input);
        }
        if (omniScript != null) {
            outputs.add(new BTCTransaction.Output(
                    0,
                    omniScript
            ));
        }
        return outputs.toArray(new BTCTransaction.Output[0]);
    }

    public static BTCTransaction.Input[] generateTransactionInput(List<UTXO> utxoList) {
        List<BTCTransaction.Input> inputs = new ArrayList<>(utxoList.size());

        for (UTXO utxo : utxoList) {
            int sequence = 0xffffffff;
            BTCTransaction.Input input = new BTCTransaction.Input(
                    new BTCTransaction.OutPoint(HexUtils.fromHex(utxo.getTxid()), utxo.getVout()),
                    new Script(HexUtils.fromHex(utxo.getScriptPubKey())),
                    sequence
            );
            inputs.add(input);
        }

        return inputs.toArray(new BTCTransaction.Input[0]);
    }

    public interface FeeCallback {
        public void onFees(String fee);
    }
}

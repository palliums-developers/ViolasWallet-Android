package com.violas.wallet.biz.btc;

import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;
import com.violas.wallet.repository.http.btcBrowser.request.FeeEstimateRequest;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class FeeManager {
    private Integer minFee;
    private Integer maxFee;

    public BigDecimal calculateFee(List<UTXO> utxoList, int toAddressSize) {
        return calculateFee(utxoList, toAddressSize, 60);
    }

    public BigDecimal calculateFee(List<UTXO> utxoList, int toAddressSize, int progress) {
        if (minFee == null || maxFee == null) {
            loadFee();
        }
        return calculate(utxoList, toAddressSize, progress);
    }

    private void loadFee() {
        new FeeEstimateRequest().estimateFee()
                .subscribe(new Observer<FeeEstimateRequest.FeesBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FeeEstimateRequest.FeesBean fees) {
                        minFee = fees.hourFee;
                        maxFee = fees.fastestFee;
                    }

                    @Override
                    public void onError(Throwable e) {
                        checkLoadFee();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void checkLoadFee() {
        if (minFee == null || maxFee == null) {
            minFee = 10;
            maxFee = 20;
        }
    }

    private BigDecimal calculate(List<UTXO> utxoList, int toAddressSize, int progress) {
        checkLoadFee();

        long totalSize = calculateTxSize(utxoList, toAddressSize);

        BigDecimal subtract = new BigDecimal((maxFee - minFee) + "");
        BigDecimal multiply = subtract.multiply(new BigDecimal((progress / 100D) + ""));

        return new BigDecimal(minFee + "").add(multiply).multiply(new BigDecimal(totalSize + "")).divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_UP);
    }

    public static long calculateTxSize(List<UTXO> utxoList, int toAddressList) {
        long totalSize = 10;
        totalSize += calculateInput(utxoList);
        totalSize += calculateOutput(toAddressList);
        return totalSize;
    }

    private static long calculateOutput(int toAddressList) {
        return toAddressList * 34;
    }

    private static long calculateInput(List<UTXO> utxoList) {
        if (utxoList == null) {
            return 0;
        }
        for (UTXO utxo : utxoList) {
            switch (utxo.getUtxoType()) {
                case UTXO.P2PKH:
                    return 147;
                case UTXO.P2SH:
                    // TODO: 2019/3/19 暂时不支持 P2SH
                    return 0;
                default:
                case UTXO.NONE:
                    return 147;
            }
        }
        return 0;
    }
}

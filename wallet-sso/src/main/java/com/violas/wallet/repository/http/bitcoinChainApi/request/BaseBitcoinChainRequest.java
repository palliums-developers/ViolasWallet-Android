package com.violas.wallet.repository.http.bitcoinChainApi.request;


import com.violas.wallet.repository.http.bitcoinChainApi.bean.TransactionBean;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.Observable;

public interface BaseBitcoinChainRequest {

    Observable<List<UTXO>> getUtxo(final String address);

    Observable<BigDecimal> getBalance(final String address);

    Observable<String> pushTx(final String tx);

    Observable<TransactionBean> getTranscation(final String TXHash);
}

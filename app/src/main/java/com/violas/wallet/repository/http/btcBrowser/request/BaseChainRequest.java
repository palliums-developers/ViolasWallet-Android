package com.violas.wallet.repository.http.btcBrowser.request;


import com.violas.wallet.repository.http.btcBrowser.bean.TransactionBean;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.Observable;

public interface BaseChainRequest {

    Observable<List<UTXO>> getUtxo(final String address);

    Observable<BigDecimal> getBalance(final String address);

    Observable<String> pushTx(final String tx);

    Observable<TransactionBean> getTranscation(final String TXHash);
}

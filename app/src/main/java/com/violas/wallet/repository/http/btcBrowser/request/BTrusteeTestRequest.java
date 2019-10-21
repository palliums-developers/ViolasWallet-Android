package com.violas.wallet.repository.http.btcBrowser.request;

import com.violas.wallet.repository.http.btcBrowser.bean.TransactionBean;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;
import com.violas.wallet.repository.http.interceptor.BTrusteeInterceptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class BTrusteeTestRequest implements BaseChainRequest {
    private static Api utxoRequest;

    protected static Retrofit sRetrofit;

    static {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor(new BTrusteeInterceptor())
                .build();

        sRetrofit = new Retrofit.Builder()
                .baseUrl("http://47.52.195.50/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        utxoRequest = sRetrofit.create(Api.class);
    }

    public static Api getRequest() {
        return utxoRequest;
    }

    public interface Api {
        @GET("utxo/{address}")
        Observable<List<BTrusteeRequest.BtUtxoBean>> getUTXO(@Path("address") String address);

        @GET("balance/{address}")
        Observable<BTrusteeRequest.BtBalanceBean> getBalance(@Path("address") String address);

        @GET("gettransaction/{txhash}")
        Observable<BTrusteeRequest.BtTranceBean> getTx(@Path("txhash") String txhash);

        @GET("sendrawtransaction/{tx}")
        Observable<BTrusteeRequest.PushTxBean> pushTx(@Path("tx") String tx);
    }

    public Observable<List<UTXO>> getUtxo(final String address) {
        return BTrusteeTestRequest.getRequest().getUTXO(address)
                .map(new Function<List<BTrusteeRequest.BtUtxoBean>, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(List<BTrusteeRequest.BtUtxoBean> btUtxoBeans) throws Exception {
                        return BTrusteeRequest.parse(btUtxoBeans, address);
                    }
                });
    }

    public Observable<BigDecimal> getBalance(final String address) {
        return BTrusteeTestRequest.getRequest().getBalance(address)
                .map(new Function<BTrusteeRequest.BtBalanceBean, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BTrusteeRequest.BtBalanceBean balanceBlockCypher) throws Exception {
                        return new BigDecimal(balanceBlockCypher.total + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP);
                    }
                });
    }

    @Override
    public Observable<String> pushTx(String tx) {
        return BTrusteeTestRequest.getRequest().pushTx(tx)
                .map(new Function<BTrusteeRequest.PushTxBean, String>() {
                    @Override
                    public String apply(BTrusteeRequest.PushTxBean txrefs) throws Exception {
                        if (txrefs.result == null) {
                            throw new RuntimeException();
                        }
                        return txrefs.result;
                    }
                });
    }

    public Observable<TransactionBean> getTranscation(final String TXHash) {
        return BTrusteeTestRequest.getRequest().getTx(TXHash)
                .map(new Function<BTrusteeRequest.BtTranceBean, TransactionBean>() {
                    @Override
                    public TransactionBean apply(BTrusteeRequest.BtTranceBean btTrance) throws Exception {
                        return BTrusteeRequest.parse(btTrance);
                    }
                });
    }
}
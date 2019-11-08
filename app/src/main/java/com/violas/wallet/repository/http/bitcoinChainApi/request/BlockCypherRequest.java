package com.violas.wallet.repository.http.bitcoinChainApi.request;

import com.google.gson.Gson;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TXBeanCypher;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TransactionBean;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXOBlockCypher;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.BalanceBlockCypherResponse;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.PushTxBlockCypherResponse;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.UTXOBlockCypherResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class BlockCypherRequest extends BaseRequest<BlockCypherRequest.Api> implements BaseBitcoinChainRequest {
    @Override
    public String requestUrl() {
        return "https://api.blockcypher.com/v1/btc/test3/";
    }

    @Override
    protected Class requestApi() {
        return Api.class;
    }

    public interface Api {
        @GET("addrs/{address}?unspentOnly=true&includeScript=true")
        Observable<UTXOBlockCypherResponse> getUTXO(@Path("address") String address);

        @GET("addrs/{address}/balance")
        Observable<BalanceBlockCypherResponse> getBalance(@Path("address") String address);

        @GET("txs/{txhash}")
        Observable<TXBeanCypher> getTx(@Path("txhash") String txhash);

        @POST("txs/push")
        Observable<PushTxBlockCypherResponse> pushTx(@Body RequestBody tx);
    }


    private Observable<PushTxBlockCypherResponse> pushTX(String tx) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(new TxBean(tx)));
        return getRequest().pushTx(requestBody);
    }

    public Observable<List<UTXO>> getUtxo(final String address) {
        return getRequest().getUTXO(address)
                .map(new Function<UTXOBlockCypherResponse, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(UTXOBlockCypherResponse utxoBlockCypherResponse) throws Exception {
                        return parse(utxoBlockCypherResponse.txrefs, address);
                    }
                });
    }

    public Observable<BigDecimal> getBalance(final String address) {
        return getRequest().getBalance(address)
                .map(new Function<BalanceBlockCypherResponse, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BalanceBlockCypherResponse balanceBlockCypher) throws Exception {
                        return new BigDecimal(balanceBlockCypher.getBalance() + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN);
                    }
                });
    }

    public Observable<String> pushTx(final String tx) {
        return pushTX(tx)
                .map(new Function<PushTxBlockCypherResponse, String>() {
                    @Override
                    public String apply(PushTxBlockCypherResponse txBlockCypherResponse) throws Exception {
                        return txBlockCypherResponse.tx.hash;
                    }
                });
    }

    public Observable<TransactionBean> getTranscation(final String TXHash) {
        return getRequest().getTx(TXHash)
                .map(new Function<TXBeanCypher, TransactionBean>() {
                    @Override
                    public TransactionBean apply(TXBeanCypher txBeanCypher) throws Exception {
                        return parse(txBeanCypher);
                    }
                });
    }

    private List<UTXO> parse(List<UTXOBlockCypher> beans, String address) {
        List<UTXO> utxos = new ArrayList<>(beans.size());

        for (UTXOBlockCypher bean : beans) {
            utxos.add(parse(bean, address));
        }
        return utxos;
    }

    private TransactionBean parse(TXBeanCypher btTrance) {
        // String blockhash, long blocktime, int confirmations, String hash, String hex, long locktime, long time, int version
        return new TransactionBean(btTrance.getBlock_hash(), btTrance.getLock_time(), btTrance.getConfirmations(), btTrance.getHash(), btTrance.getHex(), btTrance.getLock_time(), btTrance.getVer());
    }


    private UTXO parse(UTXOBlockCypher bean, String address) {
        //String address, String txid, int vout, String scriptPubKey, double amount, int height, int confirmations
        return new UTXO(address, bean.getTx_hash(), bean.getTx_output_n(), bean.getScript(), bean.getValue() / 100000000d, bean.getBlock_height(), bean.getConfirmations());
    }

    private static class TxBean {
        private String tx;

        TxBean(String tx) {
            this.tx = tx;
        }
    }
}

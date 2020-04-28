package com.violas.wallet.repository.http.bitcoinChainApi.request;

import com.google.gson.Gson;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TXBeanCypherDTO;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TransactionBean;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.BalanceBlockCypherDTO;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.PushTxBlockCypherDTO;
import com.violas.wallet.repository.http.bitcoinChainApi.respones.UTXOBlockCypherDTO;

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
        Observable<UTXOBlockCypherDTO> getUTXO(@Path("address") String address);

        @GET("addrs/{address}/balance")
        Observable<BalanceBlockCypherDTO> getBalance(@Path("address") String address);

        @GET("txs/{txhash}")
        Observable<TXBeanCypherDTO> getTx(@Path("txhash") String txhash);

        @POST("txs/push")
        Observable<PushTxBlockCypherDTO> pushTx(@Body RequestBody tx);
    }


    private Observable<PushTxBlockCypherDTO> pushTX(String tx) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(new TxDTO(tx)));
        return getRequest().pushTx(requestBody);
    }

    public Observable<List<UTXO>> getUtxo(final String address) {
        return getRequest().getUTXO(address)
                .map(new Function<UTXOBlockCypherDTO, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(UTXOBlockCypherDTO utxoBlockCypherResponse) throws Exception {
                        return parse(utxoBlockCypherResponse.txrefs, address);
                    }
                });
    }

    public Observable<BigDecimal> getBalance(final String address) {
        return getRequest().getBalance(address)
                .map(new Function<BalanceBlockCypherDTO, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BalanceBlockCypherDTO balanceBlockCypher) throws Exception {
                        return new BigDecimal(balanceBlockCypher.getBalance() + "");//.divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN);
                    }
                });
    }

    public Observable<String> pushTx(final String tx) {
        return pushTX(tx)
                .map(new Function<PushTxBlockCypherDTO, String>() {
                    @Override
                    public String apply(PushTxBlockCypherDTO txBlockCypherResponse) throws Exception {
                        return txBlockCypherResponse.tx.hash;
                    }
                });
    }

    public Observable<TransactionBean> getTranscation(final String TXHash) {
        return getRequest().getTx(TXHash)
                .map(new Function<TXBeanCypherDTO, TransactionBean>() {
                    @Override
                    public TransactionBean apply(TXBeanCypherDTO txBeanCypherDTO) throws Exception {
                        return parse(txBeanCypherDTO);
                    }
                });
    }

    private List<UTXO> parse(List<com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXOBlockCypherDTO> beans, String address) {
        List<UTXO> utxos = new ArrayList<>(beans.size());

        for (com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXOBlockCypherDTO bean : beans) {
            utxos.add(parse(bean, address));
        }
        return utxos;
    }

    private TransactionBean parse(TXBeanCypherDTO btTrance) {
        // String blockhash, long blocktime, int confirmations, String hash, String hex, long locktime, long time, int version
        return new TransactionBean(btTrance.getBlock_hash(), btTrance.getLock_time(), btTrance.getConfirmations(), btTrance.getHash(), btTrance.getHex(), btTrance.getLock_time(), btTrance.getVer());
    }


    private UTXO parse(com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXOBlockCypherDTO bean, String address) {
        //String address, String txid, int vout, String scriptPubKey, double amount, int height, int confirmations
        return new UTXO(address, bean.getTx_hash(), bean.getTx_output_n(), bean.getScript(), bean.getValue() / 100000000d, bean.getBlock_height(), bean.getConfirmations());
    }

    private static class TxDTO {
        private String tx;

        TxDTO(String tx) {
            this.tx = tx;
        }
    }
}

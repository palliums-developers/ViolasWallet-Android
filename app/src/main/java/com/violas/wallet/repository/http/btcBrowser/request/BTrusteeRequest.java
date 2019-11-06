package com.violas.wallet.repository.http.btcBrowser.request;

import com.google.gson.annotations.SerializedName;
import com.violas.wallet.repository.http.btcBrowser.bean.TransactionBean;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;
import com.violas.wallet.repository.http.interceptor.BTrusteeInterceptor;

import java.math.BigDecimal;
import java.util.ArrayList;
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

public class BTrusteeRequest implements BaseChainRequest {
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
                .baseUrl("http://18.220.66.235/")
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
        Observable<List<BtUtxoBean>> getUTXO(@Path("address") String address);

        @GET("balance/{address}")
        Observable<BtBalanceBean> getBalance(@Path("address") String address);

        @GET("gettransaction/{txhash}")
        Observable<BtTranceBean> getTx(@Path("txhash") String txhash);

        @GET("sendrawtransaction/{tx}")
        Observable<PushTxBean> pushTx(@Path("tx") String tx);
    }

    public Observable<List<UTXO>> getUtxo(final String address) {
        return BTrusteeRequest.getRequest().getUTXO(address)
                .map(new Function<List<BtUtxoBean>, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(List<BtUtxoBean> btUtxos) throws Exception {
                        return parse(btUtxos, address);
                    }
                });
    }

    public Observable<BigDecimal> getBalance(final String address) {
        return BTrusteeRequest.getRequest().getBalance(address)
                .map(new Function<BtBalanceBean, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BtBalanceBean balanceBlockCypher) throws Exception {
                        return new BigDecimal(balanceBlockCypher.total + "");
                    }
                });
    }

    @Override
    public Observable<String> pushTx(String tx) {
        return BTrusteeRequest.getRequest().pushTx(tx)
                .map(new Function<PushTxBean, String>() {
                    @Override
                    public String apply(PushTxBean txrefs) throws Exception {
                        if (txrefs.result == null) {
                            throw new RuntimeException();
                        }
                        return txrefs.result;
                    }
                });
    }

    public Observable<TransactionBean> getTranscation(final String TXHash) {
        return BTrusteeRequest.getRequest().getTx(TXHash)
                .map(new Function<BtTranceBean, TransactionBean>() {
                    @Override
                    public TransactionBean apply(BtTranceBean btTrance) throws Exception {
                        return parse(btTrance);
                    }
                });
    }

    static List<UTXO> parse(List<BtUtxoBean> beans, String address) {
        List<UTXO> utxos = new ArrayList<>(beans.size());

        for (BtUtxoBean bean : beans) {
            utxos.add(parse(bean, address));
        }
        return utxos;
    }


    static UTXO parse(BtUtxoBean bean, String address) {
        //String address, String txid, int vout, String scriptPubKey, double amount, int height, int confirmations
        BigDecimal divide = new BigDecimal(bean.value + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP);
        return new UTXO(address, bean.tx_hash, bean.tx_output_n, bean.hex, divide.doubleValue(), 0, bean.confirmations);
    }

    static TransactionBean parse(BtTranceBean btTrance) {
        // String blockhash, long blocktime, int confirmations, String hash, String hex, long locktime, long time, int version
        //return new TransactionBean(btTrance.blockhash, btTrance.blocktime, btTrance.confirmations, btTrance.hash, btTrance.hex, btTrance.locktime, btTrance.version);
        return new TransactionBean(btTrance);
    }

    static class BtBalanceBean {
        /**
         * address : mfmqkQYFeDuEzuaFjmB8UKRN15ook5NhcY
         * confimed : 160187058
         * total : 160187058
         * tx count : 28637
         * unconfrimed : 0
         */

        public String address;
        public int confimed;
        public int total;
        @SerializedName("tx count")
        public int tx_count;
        public int unconfrimed;
    }

    static class BtUtxoBean {
        /**
         * asm : OP_DUP OP_HASH160 02d0c6807fd78027b7a0278e904425c699342fdd OP_EQUALVERIFY OP_CHECKSIG
         * confirmations : 94637
         * height : 1449387
         * hex : 76a91402d0c6807fd78027b7a0278e904425c699342fdd88ac
         * tx_hash : c934e7680c49bc443244e4827ed5c029d02297d2e1b41499c10c7dd57736948e
         * tx_output_n : 0
         * type : pubkeyhash
         * value : 5460
         */

        public String asm;
        public int confirmations;
        public int height;
        public String hex;
        public String tx_hash;
        public int tx_output_n;
        public String type;
        public int value;
    }

    public static class PushTxBean {
        /**
         * result : a5c3e500bbadebb3bc368eae64148f92564404c339fb12b944f11198aa135266
         * error : null
         * id : curltest
         */

        public String result;
        public Object error;
        public String id;
    }

    public static class BtTranceBean {

        /**
         * error : null
         * id : null
         * result : {"blockhash":"00000000000001828f7679566e8175eab43eae120c4830bb4ec5d2b7b0ba4cb0","blocktime":1556420303,"confirmations":6014,"hash":"3388364d590f3be07bf260fc2c8ad9f6b619564b299ab31cb302f15b8a4f698c","hex":"01000000011b2333a0335843f80d922a5b971acdd92206ee33c2896e5d7c8d7bf3b8e5f781000000006a473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303ffffffff0393cb0000000000001976a9140520252a3f13e5f730583912395cc72e022be7c388ac0000000000000000166a146f6d6e69000000000000000200000000004c4b4022020000000000001976a91487cde79bbd94f3a4c78a4d02702ab6b54d98d1bb88ac00000000","locktime":0,"size":256,"time":1556420303,"txid":"3388364d590f3be07bf260fc2c8ad9f6b619564b299ab31cb302f15b8a4f698c","version":1,"vin":[{"scriptSig":{"asm":"3044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa35[ALL] 0359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303","hex":"473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303"},"sequence":4294967295,"txid":"81f7e5b8f37b8d7c5d6e89c233ee0622d9cd1a975b2a920df8435833a033231b","vout":0}],"vout":[{"n":0,"scriptPubKey":{"addresses":["mfz4BVuPb1NY34ctfYLaQdZXSrCKGPysYL"],"asm":"OP_DUP OP_HASH160 0520252a3f13e5f730583912395cc72e022be7c3 OP_EQUALVERIFY OP_CHECKSIG","hex":"76a9140520252a3f13e5f730583912395cc72e022be7c388ac","reqSigs":1,"type":"pubkeyhash"},"value":5.2115E-4},{"n":1,"scriptPubKey":{"asm":"OP_RETURN 6f6d6e69000000000000000200000000004c4b40","hex":"6a146f6d6e69000000000000000200000000004c4b40","type":"nulldata"},"value":0},{"n":2,"scriptPubKey":{"addresses":["msu2BTk4pvu6AhoN5STxGePzu5nFo3xdnt"],"asm":"OP_DUP OP_HASH160 87cde79bbd94f3a4c78a4d02702ab6b54d98d1bb OP_EQUALVERIFY OP_CHECKSIG","hex":"76a91487cde79bbd94f3a4c78a4d02702ab6b54d98d1bb88ac","reqSigs":1,"type":"pubkeyhash"},"value":5.46E-6}],"vsize":256,"weight":1024}
         */

        public Object error;
        public Object id;
        public ResultBean result;

        public static class ResultBean {
            /**
             * blockhash : 00000000000001828f7679566e8175eab43eae120c4830bb4ec5d2b7b0ba4cb0
             * blocktime : 1556420303
             * confirmations : 6014
             * hash : 3388364d590f3be07bf260fc2c8ad9f6b619564b299ab31cb302f15b8a4f698c
             * hex : 01000000011b2333a0335843f80d922a5b971acdd92206ee33c2896e5d7c8d7bf3b8e5f781000000006a473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303ffffffff0393cb0000000000001976a9140520252a3f13e5f730583912395cc72e022be7c388ac0000000000000000166a146f6d6e69000000000000000200000000004c4b4022020000000000001976a91487cde79bbd94f3a4c78a4d02702ab6b54d98d1bb88ac00000000
             * locktime : 0
             * size : 256
             * time : 1556420303
             * txid : 3388364d590f3be07bf260fc2c8ad9f6b619564b299ab31cb302f15b8a4f698c
             * version : 1
             * vin : [{"scriptSig":{"asm":"3044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa35[ALL] 0359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303","hex":"473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303"},"sequence":4294967295,"txid":"81f7e5b8f37b8d7c5d6e89c233ee0622d9cd1a975b2a920df8435833a033231b","vout":0}]
             * vout : [{"n":0,"scriptPubKey":{"addresses":["mfz4BVuPb1NY34ctfYLaQdZXSrCKGPysYL"],"asm":"OP_DUP OP_HASH160 0520252a3f13e5f730583912395cc72e022be7c3 OP_EQUALVERIFY OP_CHECKSIG","hex":"76a9140520252a3f13e5f730583912395cc72e022be7c388ac","reqSigs":1,"type":"pubkeyhash"},"value":5.2115E-4},{"n":1,"scriptPubKey":{"asm":"OP_RETURN 6f6d6e69000000000000000200000000004c4b40","hex":"6a146f6d6e69000000000000000200000000004c4b40","type":"nulldata"},"value":0},{"n":2,"scriptPubKey":{"addresses":["msu2BTk4pvu6AhoN5STxGePzu5nFo3xdnt"],"asm":"OP_DUP OP_HASH160 87cde79bbd94f3a4c78a4d02702ab6b54d98d1bb OP_EQUALVERIFY OP_CHECKSIG","hex":"76a91487cde79bbd94f3a4c78a4d02702ab6b54d98d1bb88ac","reqSigs":1,"type":"pubkeyhash"},"value":5.46E-6}]
             * vsize : 256
             * weight : 1024
             */

            public String blockhash;
            public long blocktime;
            public long confirmations;
            public String hash;
            public String hex;
            public long locktime;
            public int size;
            public int time;
            public String txid;
            public int version;
            public int vsize;
            public int weight;
            public List<VinBean> vin;
            public List<VoutBean> vout;

            public static class VinBean {
                /**
                 * scriptSig : {"asm":"3044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa35[ALL] 0359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303","hex":"473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303"}
                 * sequence : 4294967295
                 * txid : 81f7e5b8f37b8d7c5d6e89c233ee0622d9cd1a975b2a920df8435833a033231b
                 * vout : 0
                 */

                public ScriptSigBean scriptSig;
                public long sequence;
                public String txid;
                public int vout;

                public static class ScriptSigBean {
                    /**
                     * asm : 3044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa35[ALL] 0359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303
                     * hex : 473044022054419de1d6464cff49f662b585d98d8e49599379c88df83f69be91378ea954ca022045858b4f5c6abce7d01bc184da6448db66ab792551fb6a9782d57c854e83aa3501210359eadaef1f2df893f69d4fb7bed76d67a6c1d80a4234009926242cdd61983303
                     */

                    public String asm;
                    public String hex;
                }
            }

            public static class VoutBean {
                /**
                 * n : 0
                 * scriptPubKey : {"addresses":["mfz4BVuPb1NY34ctfYLaQdZXSrCKGPysYL"],"asm":"OP_DUP OP_HASH160 0520252a3f13e5f730583912395cc72e022be7c3 OP_EQUALVERIFY OP_CHECKSIG","hex":"76a9140520252a3f13e5f730583912395cc72e022be7c388ac","reqSigs":1,"type":"pubkeyhash"}
                 * value : 5.2115E-4
                 */

                public int n;
                public ScriptPubKeyBean scriptPubKey;
                public double value;

                public static class ScriptPubKeyBean {
                    /**
                     * addresses : ["mfz4BVuPb1NY34ctfYLaQdZXSrCKGPysYL"]
                     * asm : OP_DUP OP_HASH160 0520252a3f13e5f730583912395cc72e022be7c3 OP_EQUALVERIFY OP_CHECKSIG
                     * hex : 76a9140520252a3f13e5f730583912395cc72e022be7c388ac
                     * reqSigs : 1
                     * type : pubkeyhash
                     */

                    public String asm;
                    public String hex;
                    public long reqSigs;
                    public String type;
                    public List<String> addresses;
                }
            }
        }
    }
}
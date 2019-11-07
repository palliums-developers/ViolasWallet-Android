package com.violas.wallet.repository.http.btcBrowser.request;

import com.google.gson.Gson;
import com.violas.wallet.repository.http.btcBrowser.bean.TransactionBean;
import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 比特大陆测试 UTXO 服务器
 */
public class BTCTestRequest extends BaseRequest implements BaseChainRequest {
    private static Api utxoRequest;

    static {
        utxoRequest = sRetrofit.create(Api.class);
    }

    public static Api getRequest() {
        return utxoRequest;
    }

    public interface Api {

        @GET("https://tchain.api.btc.com/v3/address/{address}/unspent")
        Observable<UnSpentBean> getUTXO(@Path("address") String address);

        @GET("https://tchain.api.btc.com/v3/address/{address}")
        Observable<BalanceBean> getBalance(@Path("address") String address);

        @GET("https://tchain.api.btc.com/v3/tx/{txhash}?verbose=3")
        Observable<TranceBean> getTx(@Path("txhash") String txhash);

//        @POST("https://tchain.api.btc.com/v3/tools/tx-publish")
//        @FormUrlEncoded
//        Observable<BTCRequest.PushTxBean> pushTx(@Field("rawhex") String tx);

        @POST("https://tchain.api.btc.com/v3/tools/tx-publish")
        Observable<BTCRequest.PushTxBean> pushTx(@Body RequestBody tx);

//        @GET("http://223.99.243.185:5000/sendrawtransaction/{tx}")
//        Observable<BTrusteeRequest.PushTxBean> pushTx(@Path("tx") String tx);

//        @GET("http://13.68.141.242:5000/sendrawtransaction/{tx}")
//        Observable<BTrusteeRequest.PushTxBean> pushTx(@Path("tx") String tx);
    }

    @Override
    public Observable<List<UTXO>> getUtxo(String address) {
        return BTCTestRequest.getRequest().getUTXO(address)
                .map(new Function<UnSpentBean, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(UnSpentBean unSpent) throws Exception {
                        if (unSpent.data == null) {
                            return new ArrayList<>(0);
                        }
                        return parse(unSpent, address);
                    }
                });
    }


    @Override
    public Observable<BigDecimal> getBalance(String address) {
        return BTCTestRequest.getRequest().getBalance(address)
                .map(new Function<BalanceBean, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BalanceBean balanceBlockCypher) throws Exception {
                        if (balanceBlockCypher.data == null) return new BigDecimal(0);
                        return new BigDecimal(balanceBlockCypher.data.balance + "");
                    }
                });
    }

    @Override
    public Observable<String> pushTx(String tx) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(new BTCRequest.TxBean(tx)));
        return BTCTestRequest.getRequest().pushTx(requestBody)
                .map(new Function<BTCRequest.PushTxBean, String>() {
                    @Override
                    public String apply(BTCRequest.PushTxBean txrefs) throws Exception {
                        if (txrefs.data == null || txrefs.err_no != 0) {
                            throw new RuntimeException();
                        }
                        return txrefs.data;
                    }
                });
    }

//    @Override
//    public Observable<String> pushTx(String tx) {
//        return BTCTestRequest.getRequest().pushTx(tx)
//                .map(new Function<BTCRequest.PushTxBean, String>() {
//                    @Override
//                    public String apply(BTCRequest.PushTxBean txrefs) throws Exception {
//                        if (txrefs.data == null || txrefs.err_no != 0) {
//                            throw new RuntimeException();
//                        }
//                        return txrefs.data;
//                    }
//                });
//    }

    @Override
    public Observable<TransactionBean> getTranscation(String TXHash) {
        return BTCTestRequest.getRequest().getTx(TXHash)
                .map(new Function<TranceBean, TransactionBean>() {
                    @Override
                    public TransactionBean apply(TranceBean btTrance) throws Exception {
                        return parse(btTrance);
                    }
                });
    }

    private TransactionBean parse(TranceBean btTrance) {
        return new TransactionBean(btTrance.data);
    }

    private List<UTXO> parse(UnSpentBean unSpent, String address) {
        List<UTXO> utxos = new ArrayList<>(unSpent.data.list.size());

        for (UnSpentBean.DataBean.ListBean list : unSpent.data.list) {
            try {
                utxos.add(parse(list, address));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return utxos;
    }

    private UTXO parse(UnSpentBean.DataBean.ListBean bean, String address) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final TransactionBean[] transaction = new TransactionBean[1];
        BlockChainRequest.get().getTranscation(bean.tx_hash)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<TransactionBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(TransactionBean transactionBean) {
                        transaction[0] = transactionBean;
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (transaction[0] == null) {
            throw new RuntimeException();
        }
        BigDecimal divide = new BigDecimal(bean.value + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP);
        return new UTXO(address, bean.tx_hash, bean.tx_output_n, transaction[0].getVout().get(bean.tx_output_n).getScriptPubKey().getHex(), divide.doubleValue(), 0, bean.confirmations);
    }

    public static class TranceBean {

        /**
         * err_no : 0
         * data : {"confirmations":0,"block_height":-1,"block_hash":"","block_time":0,"created_at":1553846936,"fee":140410985503813,"hash":"6b05e2e151ab7821ac55fcc2cd7c90c85aca5b21c4291203311580cc2d0e4acc","inputs_count":1,"inputs_value":140410986576833,"is_coinbase":false,"is_double_spend":false,"is_sw_tx":false,"weight":661,"vsize":166,"witness_hash":"91c96f3563c8e9a2e6b5269ba6e5d05fdd1dd1fc9c888557a725c4be3e832bae","lock_time":1486440,"outputs_count":2,"outputs_value":1073020,"size":247,"sigops":1,"version":2,"inputs":[{"prev_addresses":["2NF9jCvSfrrxDgnJAa2Z6dvKb5MxhC1ixmY"],"prev_position":0,"prev_tx_hash":"03c9276d60a682ada14a585a2fc9fdfcef23cd6b9a36268d21c4a640102fd4f9","prev_type":"P2SH","prev_value":1073186,"sequence":4294967294,"script_asm":"00145f3c473ae8b6551dc9c13c133f116054c3422aa2","script_hex":"1600145f3c473ae8b6551dc9c13c133f116054c3422aa2","witness":["304402204123d14626b1892fd229e9f723a4c1f11332e3eda6a47ceaef56e0205c23fd6e02201a799ed27cd8335d2da6c4928302ae7692aaa30a43769a503022f913ab53bdf701","021fda2e75bc6180be2389417f27f27dd55d87ad81f3569bad058937e504360a62"]}],"outputs":[{"addresses":["2NADuTWkyySGfdm9VDpMeiaxnQ2EpEczr1r"],"value":10000,"type":"P2SH","script_asm":"OP_HASH160 ba3a40fd47a44e72466aee8935c53a70999f2771 OP_EQUAL","script_hex":"a914ba3a40fd47a44e72466aee8935c53a70999f277187","spent_by_tx":null,"spent_by_tx_position":-1},{"addresses":["2MuciqLViqveoMrfGpN8GhdPfeaw6sYFwRt"],"value":1063020,"type":"P2SH","script_asm":"OP_HASH160 1a0110e3e283d8b7b97a12f0aa3c5efc387d361d OP_EQUAL","script_hex":"a9141a0110e3e283d8b7b97a12f0aa3c5efc387d361d87","spent_by_tx":null,"spent_by_tx_position":-1}]}
         */

        public int err_no;
        public DataBean data;

        public static class DataBean {
            /**
             * confirmations : 0
             * block_height : -1
             * block_hash :
             * block_time : 0
             * created_at : 1553846936
             * fee : 140410985503813
             * hash : 6b05e2e151ab7821ac55fcc2cd7c90c85aca5b21c4291203311580cc2d0e4acc
             * inputs_count : 1
             * inputs_value : 140410986576833
             * is_coinbase : false
             * is_double_spend : false
             * is_sw_tx : false
             * weight : 661
             * vsize : 166
             * witness_hash : 91c96f3563c8e9a2e6b5269ba6e5d05fdd1dd1fc9c888557a725c4be3e832bae
             * lock_time : 1486440
             * outputs_count : 2
             * outputs_value : 1073020
             * size : 247
             * sigops : 1
             * version : 2
             * inputs : [{"prev_addresses":["2NF9jCvSfrrxDgnJAa2Z6dvKb5MxhC1ixmY"],"prev_position":0,"prev_tx_hash":"03c9276d60a682ada14a585a2fc9fdfcef23cd6b9a36268d21c4a640102fd4f9","prev_type":"P2SH","prev_value":1073186,"sequence":4294967294,"script_asm":"00145f3c473ae8b6551dc9c13c133f116054c3422aa2","script_hex":"1600145f3c473ae8b6551dc9c13c133f116054c3422aa2","witness":["304402204123d14626b1892fd229e9f723a4c1f11332e3eda6a47ceaef56e0205c23fd6e02201a799ed27cd8335d2da6c4928302ae7692aaa30a43769a503022f913ab53bdf701","021fda2e75bc6180be2389417f27f27dd55d87ad81f3569bad058937e504360a62"]}]
             * outputs : [{"addresses":["2NADuTWkyySGfdm9VDpMeiaxnQ2EpEczr1r"],"value":10000,"type":"P2SH","script_asm":"OP_HASH160 ba3a40fd47a44e72466aee8935c53a70999f2771 OP_EQUAL","script_hex":"a914ba3a40fd47a44e72466aee8935c53a70999f277187","spent_by_tx":null,"spent_by_tx_position":-1},{"addresses":["2MuciqLViqveoMrfGpN8GhdPfeaw6sYFwRt"],"value":1063020,"type":"P2SH","script_asm":"OP_HASH160 1a0110e3e283d8b7b97a12f0aa3c5efc387d361d OP_EQUAL","script_hex":"a9141a0110e3e283d8b7b97a12f0aa3c5efc387d361d87","spent_by_tx":null,"spent_by_tx_position":-1}]
             */

            public long confirmations;
            public int block_height;
            public String block_hash;
            public int block_time;
            public int created_at;
            public long fee;
            public String hash;
            public int inputs_count;
            public long inputs_value;
            public boolean is_coinbase;
            public boolean is_double_spend;
            public boolean is_sw_tx;
            public int weight;
            public int vsize;
            public String witness_hash;
            public int lock_time;
            public int outputs_count;
            public long outputs_value;
            public int size;
            public int sigops;
            public int version;
            public List<InputsBean> inputs;
            public List<OutputsBean> outputs;

            public static class InputsBean {
                /**
                 * prev_addresses : ["2NF9jCvSfrrxDgnJAa2Z6dvKb5MxhC1ixmY"]
                 * prev_position : 0
                 * prev_tx_hash : 03c9276d60a682ada14a585a2fc9fdfcef23cd6b9a36268d21c4a640102fd4f9
                 * prev_type : P2SH
                 * prev_value : 1073186
                 * sequence : 4294967294
                 * script_asm : 00145f3c473ae8b6551dc9c13c133f116054c3422aa2
                 * script_hex : 1600145f3c473ae8b6551dc9c13c133f116054c3422aa2
                 * witness : ["304402204123d14626b1892fd229e9f723a4c1f11332e3eda6a47ceaef56e0205c23fd6e02201a799ed27cd8335d2da6c4928302ae7692aaa30a43769a503022f913ab53bdf701","021fda2e75bc6180be2389417f27f27dd55d87ad81f3569bad058937e504360a62"]
                 */

                public int prev_position;
                public String prev_tx_hash;
                public String prev_type;
                public long prev_value;
                public long sequence;
                public String script_asm;
                public String script_hex;
                public List<String> prev_addresses;
                public List<String> witness;
            }

            public static class OutputsBean {
                /**
                 * addresses : ["2NADuTWkyySGfdm9VDpMeiaxnQ2EpEczr1r"]
                 * value : 10000
                 * type : P2SH
                 * script_asm : OP_HASH160 ba3a40fd47a44e72466aee8935c53a70999f2771 OP_EQUAL
                 * script_hex : a914ba3a40fd47a44e72466aee8935c53a70999f277187
                 * spent_by_tx : null
                 * spent_by_tx_position : -1
                 */

                public long value;
                public String type;
                public String script_asm;
                public String script_hex;
                public Object spent_by_tx;
                public int spent_by_tx_position;
                public List<String> addresses;
            }
        }
    }

    private static class UnSpentBean {

        /**
         * data : {"total_count":84,"page":1,"pagesize":50,"list":[{"tx_hash":"8e1ea39ee073bef438ee63d630d36433dec6ce57666af38d7337d7bb6d1b3f48","tx_output_n":0,"tx_output_n2":0,"value":95827,"confirmations":180},{"tx_hash":"e0485a9772ae47aa5707bf1cd3f5c7e3d9d96e4672b3392ffa94b11e08777b80","tx_output_n":0,"tx_output_n2":0,"value":100000,"confirmations":179},{"tx_hash":"e0485a9772ae47aa5707bf1cd3f5c7e3d9d96e4672b3392ffa94b11e08777b80","tx_output_n":1,"tx_output_n2":0,"value":16654218,"confirmations":179},{"tx_hash":"b0df43b18db5548f3e63a0d2f433ef8e42de63a2dd4246b0f0cb170aba75503b","tx_output_n":1,"tx_output_n2":0,"value":82606,"confirmations":157},{"tx_hash":"8dab01f40b058ce5dfca490e5cc53961c7a83a3e6651dce737785e6a9e970ecd","tx_output_n":1,"tx_output_n2":0,"value":21543328,"confirmations":37}]}
         * err_no : 0
         * err_msg : null
         */

        public DataBean data;
        public int err_no;
        public Object err_msg;

        public static class DataBean {
            /**
             * total_count : 84
             * page : 1
             * pagesize : 50
             * list : [{"tx_hash":"8e1ea39ee073bef438ee63d630d36433dec6ce57666af38d7337d7bb6d1b3f48","tx_output_n":0,"tx_output_n2":0,"value":95827,"confirmations":180},{"tx_hash":"e0485a9772ae47aa5707bf1cd3f5c7e3d9d96e4672b3392ffa94b11e08777b80","tx_output_n":0,"tx_output_n2":0,"value":100000,"confirmations":179},{"tx_hash":"e0485a9772ae47aa5707bf1cd3f5c7e3d9d96e4672b3392ffa94b11e08777b80","tx_output_n":1,"tx_output_n2":0,"value":16654218,"confirmations":179},{"tx_hash":"b0df43b18db5548f3e63a0d2f433ef8e42de63a2dd4246b0f0cb170aba75503b","tx_output_n":1,"tx_output_n2":0,"value":82606,"confirmations":157},{"tx_hash":"8dab01f40b058ce5dfca490e5cc53961c7a83a3e6651dce737785e6a9e970ecd","tx_output_n":1,"tx_output_n2":0,"value":21543328,"confirmations":37}]
             */

            public int total_count;
            public int page;
            public int pagesize;
            public List<ListBean> list;

            public static class ListBean {
                /**
                 * tx_hash : 8e1ea39ee073bef438ee63d630d36433dec6ce57666af38d7337d7bb6d1b3f48
                 * tx_output_n : 0
                 * tx_output_n2 : 0
                 * value : 95827
                 * confirmations : 180
                 */

                public String tx_hash;
                public int tx_output_n;
                public int tx_output_n2;
                public long value;
                public long confirmations;
            }
        }
    }

    private static class BalanceBean {

        /**
         * err_no : 0
         * data : {"address":"mousWBSN7Rsqi8qpmZp7C6VmRkBGPD5bFF","received":127665961,"sent":89189982,"balance":38475979,"tx_count":77,"unconfirmed_tx_count":0,"unconfirmed_received":0,"unconfirmed_sent":0,"unspent_tx_count":84,"first_tx":"817b3094c8e2ddb84408881c86b2fa4df84aa9c808001fb4189d697b24accdee","last_tx":"8dab01f40b058ce5dfca490e5cc53961c7a83a3e6651dce737785e6a9e970ecd"}
         */

        public int err_no;
        public DataBean data;

        public static class DataBean {
            /**
             * address : mousWBSN7Rsqi8qpmZp7C6VmRkBGPD5bFF
             * received : 127665961
             * sent : 89189982
             * balance : 38475979
             * tx_count : 77
             * unconfirmed_tx_count : 0
             * unconfirmed_received : 0
             * unconfirmed_sent : 0
             * unspent_tx_count : 84
             * first_tx : 817b3094c8e2ddb84408881c86b2fa4df84aa9c808001fb4189d697b24accdee
             * last_tx : 8dab01f40b058ce5dfca490e5cc53961c7a83a3e6651dce737785e6a9e970ecd
             */

            public String address;
            public long received;
            public long sent;
            public long balance;
            public int tx_count;
            public int unconfirmed_tx_count;
            public long unconfirmed_received;
            public long unconfirmed_sent;
            public int unspent_tx_count;
            public String first_tx;
            public String last_tx;
        }
    }

}

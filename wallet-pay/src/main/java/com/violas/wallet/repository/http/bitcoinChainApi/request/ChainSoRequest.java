package com.violas.wallet.repository.http.bitcoinChainApi.request;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TransactionBean;
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO;

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

public class ChainSoRequest extends BaseRequest<ChainSoRequest.Api> implements BaseBitcoinChainRequest {

    @Override
    public String requestUrl() {
        return "https://chain.so/api/v2/";
    }

    @Override
    protected Class requestApi() {
        return Api.class;
    }

    public interface Api {
        @GET("get_address_balance/BTCTEST/{address}")
        Observable<BalanceDTO> getBalance(@Path("address") String address);

        @GET("get_tx_unspent/BTCTEST/{address}")
        Observable<UTXORequestDTO> getUTXO(@Path("address") String address);

        @GET("get_tx/BTCTEST/{txhash}")
        Observable<TxRequestDTO> getTx(@Path("txhash") String txhash);

        @POST("send_tx/BTCTEST")
        Observable<PushTxDTO> pushTx(@Body RequestBody tx);
    }

    public Observable<List<UTXO>> getUtxo(String address) {
        return getRequest().getUTXO(address)
                .map(new Function<UTXORequestDTO, List<UTXO>>() {
                    @Override
                    public List<UTXO> apply(UTXORequestDTO txrefs) throws Exception {
                        return parse(txrefs.data.txs, txrefs.data.address);
                    }
                });
    }

    @Override
    public Observable<BigDecimal> getBalance(String address) {
        return getRequest().getBalance(address)
                .map(new Function<BalanceDTO, BigDecimal>() {
                    @Override
                    public BigDecimal apply(BalanceDTO balanceBean) throws Exception {
                        return new BigDecimal(balanceBean.data.confirmed_balance);
                    }
                });
    }

    @Override
    public Observable<String> pushTx(String tx) {
        return pushTX(tx)
                .map(new Function<PushTxDTO, String>() {
                    @Override
                    public String apply(PushTxDTO pushTx) throws Exception {
                        return pushTx.data.txid;
                    }
                });
    }

    @Override
    public Observable<TransactionBean> getTranscation(String TXHash) {
        return getRequest().getTx(TXHash)
                .map(new Function<TxRequestDTO, TransactionBean>() {
                    @Override
                    public TransactionBean apply(TxRequestDTO txRequest) throws Exception {
                        return parse(txRequest);
                    }
                });
    }

    private static class PushTxDTO {

        /**
         * status : fail
         * data : {"network":"Network is required (DOGE, DOGETEST, ...)","tx_hex":"A valid signed transaction hexadecimal string is required"}
         */

        public String status;
        public DataBean data;

        public static class DataBean {
            /**
             * network : Network is required (DOGE, DOGETEST, ...)
             * tx_hex : A valid signed transaction hexadecimal string is required
             */

            @SerializedName("network")
            public String networkX;
            public String txid;
        }
    }

    private Observable<PushTxDTO> pushTX(String tx) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(new TxDTO(tx)));
        return getRequest().pushTx(requestBody);
    }

    private TransactionBean parse(TxRequestDTO txRequest) {
        // String blockhash, long blocktime, int confirmations, String hash, String hex, long locktime, int version
        TxRequestDTO.DataBean btTrance = txRequest.data;
        return new TransactionBean(btTrance.blockhash, btTrance.time, btTrance.confirmations, btTrance.txid, btTrance.tx_hex, btTrance.locktime, btTrance.version);
    }

    private static class TxDTO {
        public TxDTO(String tx_hex) {
            this.tx_hex = tx_hex;
        }

        public String tx_hex;
    }

    public static class BalanceDTO {

        /**
         * status : success
         * data : {"network":"BTCTEST","address":"mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq","confirmed_balance":"0.00000000","unconfirmed_balance":"0.00000000"}
         */

        public String status;
        public DataBean data;

        public static class DataBean {
            /**
             * network : BTCTEST
             * address : mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq
             * confirmed_balance : 0.00000000
             * unconfirmed_balance : 0.00000000
             */

            public String network;
            public String address;
            public String confirmed_balance;
            public String unconfirmed_balance;
        }
    }

    public static class UTXORequestDTO {

        /**
         * status : success
         * data : {"network":"BTCTEST","address":"myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH","txs":[{"txid":"644599bbe22ecd608e0c53173edc9fcb00a13847eb1f06b8e33f1937260a3ef2","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.08997420","confirmations":2011,"time":1546924558},{"txid":"d4105977adaadbe94392ab0f14262f480339fd49b2e632996b5c3f1b13a18154","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.05380514","confirmations":31,"time":1548040260},{"txid":"862b0d92c47f233176e129f450440ee563137fd00df4c39b6b75af4caaee2aa9","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.16414490","confirmations":31,"time":1548040260},{"txid":"00220f3addb9c02014ab1d1afd75ba506ec1982463bbec69c0af72185c26e914","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.11159595","confirmations":31,"time":1548040260}]}
         */

        public String status;
        public DataBean data;

        public static class DataBean {
            /**
             * network : BTCTEST
             * address : myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH
             * txs : [{"txid":"644599bbe22ecd608e0c53173edc9fcb00a13847eb1f06b8e33f1937260a3ef2","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.08997420","confirmations":2011,"time":1546924558},{"txid":"d4105977adaadbe94392ab0f14262f480339fd49b2e632996b5c3f1b13a18154","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.05380514","confirmations":31,"time":1548040260},{"txid":"862b0d92c47f233176e129f450440ee563137fd00df4c39b6b75af4caaee2aa9","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.16414490","confirmations":31,"time":1548040260},{"txid":"00220f3addb9c02014ab1d1afd75ba506ec1982463bbec69c0af72185c26e914","output_no":1,"script_asm":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG","script_hex":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","value":"0.11159595","confirmations":31,"time":1548040260}]
             */

            public String network;
            public String address;
            public List<TxsBean> txs;

            public static class TxsBean {
                /**
                 * txid : 644599bbe22ecd608e0c53173edc9fcb00a13847eb1f06b8e33f1937260a3ef2
                 * output_no : 1
                 * script_asm : OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG
                 * script_hex : 76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac
                 * value : 0.08997420
                 * confirmations : 2011
                 * time : 1546924558
                 */

                public String txid;
                public int output_no;
                public String script_asm;
                public String script_hex;
                public String value;
                public int confirmations;
                public int time;
            }
        }
    }

    public static class TxRequestDTO {

        /**
         * status : success
         * data : {"network":"BTCTEST","txid":"d4105977adaadbe94392ab0f14262f480339fd49b2e632996b5c3f1b13a18154","blockhash":"000000000000013af78cbd4f01ed36fd93cc356a9b3030368f8c28dec4d32f64","confirmations":33,"time":1548040260,"inputs":[{"input_no":0,"value":"0.06390414","address":"myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH","type":"pubkeyhash","script":"304402203f1b7a438f60f8f37fc24ce999241c93d449d058f92651a782ad691f5dd71e97022002622478bb3de467b3b73de84a338b1f391d5346d7fcc62016c54a3bffce950601 02d39a933ba319e096df542615040e50e40f6c955ed26163b211d5e6450254ce60","witness":null,"from_output":{"txid":"31e45dc748c6f647aabaa366fd8df802c55f76bce1bb1d8e4849976216f3a06f","output_no":1}}],"outputs":[{"output_no":0,"value":"0.01000000","address":"2MzNrES6wNpKXqkZqDwytecHCyT7FvvGDu9","type":"scripthash","script":"OP_HASH160 4e3a0c1236a897cc02acee530fd0f6c6f50f54c9 OP_EQUAL"},{"output_no":1,"value":"0.05380514","address":"myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH","type":"pubkeyhash","script":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG"}],"tx_hex":"02000000016fa0f316629749488e1dbbe1bc765fc502f88dfd66a3baaa47f6c648c75de431010000006a47304402203f1b7a438f60f8f37fc24ce999241c93d449d058f92651a782ad691f5dd71e97022002622478bb3de467b3b73de84a338b1f391d5346d7fcc62016c54a3bffce9506012102d39a933ba319e096df542615040e50e40f6c955ed26163b211d5e6450254ce60ffffffff0240420f000000000017a9144e3a0c1236a897cc02acee530fd0f6c6f50f54c987a2195200000000001976a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac00000000","size":223,"version":2,"locktime":0}
         */

        public String status;
        public DataBean data;

        public static class DataBean {
            /**
             * network : BTCTEST
             * txid : d4105977adaadbe94392ab0f14262f480339fd49b2e632996b5c3f1b13a18154
             * blockhash : 000000000000013af78cbd4f01ed36fd93cc356a9b3030368f8c28dec4d32f64
             * confirmations : 33
             * time : 1548040260
             * inputs : [{"input_no":0,"value":"0.06390414","address":"myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH","type":"pubkeyhash","script":"304402203f1b7a438f60f8f37fc24ce999241c93d449d058f92651a782ad691f5dd71e97022002622478bb3de467b3b73de84a338b1f391d5346d7fcc62016c54a3bffce950601 02d39a933ba319e096df542615040e50e40f6c955ed26163b211d5e6450254ce60","witness":null,"from_output":{"txid":"31e45dc748c6f647aabaa366fd8df802c55f76bce1bb1d8e4849976216f3a06f","output_no":1}}]
             * outputs : [{"output_no":0,"value":"0.01000000","address":"2MzNrES6wNpKXqkZqDwytecHCyT7FvvGDu9","type":"scripthash","script":"OP_HASH160 4e3a0c1236a897cc02acee530fd0f6c6f50f54c9 OP_EQUAL"},{"output_no":1,"value":"0.05380514","address":"myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH","type":"pubkeyhash","script":"OP_DUP OP_HASH160 c7bac6db1cc9a1be72e8b058d2c59f6127099aeb OP_EQUALVERIFY OP_CHECKSIG"}]
             * tx_hex : 02000000016fa0f316629749488e1dbbe1bc765fc502f88dfd66a3baaa47f6c648c75de431010000006a47304402203f1b7a438f60f8f37fc24ce999241c93d449d058f92651a782ad691f5dd71e97022002622478bb3de467b3b73de84a338b1f391d5346d7fcc62016c54a3bffce9506012102d39a933ba319e096df542615040e50e40f6c955ed26163b211d5e6450254ce60ffffffff0240420f000000000017a9144e3a0c1236a897cc02acee530fd0f6c6f50f54c987a2195200000000001976a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac00000000
             * size : 223
             * version : 2
             * locktime : 0
             */

            public String network;
            public String txid;
            public String blockhash;
            public int confirmations;
            public int time;
            public String tx_hex;
            public int size;
            public int version;
            public int locktime;
            public List<InputsBean> inputs;
            public List<OutputsBean> outputs;

            public static class InputsBean {
                /**
                 * input_no : 0
                 * value : 0.06390414
                 * address : myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH
                 * type : pubkeyhash
                 * script : 304402203f1b7a438f60f8f37fc24ce999241c93d449d058f92651a782ad691f5dd71e97022002622478bb3de467b3b73de84a338b1f391d5346d7fcc62016c54a3bffce950601 02d39a933ba319e096df542615040e50e40f6c955ed26163b211d5e6450254ce60
                 * witness : null
                 * from_output : {"txid":"31e45dc748c6f647aabaa366fd8df802c55f76bce1bb1d8e4849976216f3a06f","output_no":1}
                 */

                public int input_no;
                public String value;
                public String address;
                public String type;
                public String script;
                public Object witness;
                public FromOutputBean from_output;

                public static class FromOutputBean {
                    /**
                     * txid : 31e45dc748c6f647aabaa366fd8df802c55f76bce1bb1d8e4849976216f3a06f
                     * output_no : 1
                     */

                    public String txid;
                    public int output_no;
                }
            }

            public static class OutputsBean {
                /**
                 * output_no : 0
                 * value : 0.01000000
                 * address : 2MzNrES6wNpKXqkZqDwytecHCyT7FvvGDu9
                 * type : scripthash
                 * script : OP_HASH160 4e3a0c1236a897cc02acee530fd0f6c6f50f54c9 OP_EQUAL
                 */

                public int output_no;
                public String value;
                public String address;
                public String type;
                public String script;
            }
        }
    }

    private static List<UTXO> parse(List<UTXORequestDTO.DataBean.TxsBean> beans, String address) {
        List<UTXO> utxos = new ArrayList<>(beans.size());

        for (UTXORequestDTO.DataBean.TxsBean bean : beans) {
            utxos.add(parse(bean, address));
        }
        return utxos;
    }


    private static UTXO parse(UTXORequestDTO.DataBean.TxsBean bean, String address) {
        // TODO: 2019/1/22 未来改进
        return new UTXO(address, bean.txid, bean.output_no, bean.script_hex, Double.valueOf(bean.value), 0, bean.confirmations);
    }
}

package com.violas.wallet.repository.http.bitcoinChainApi.respones;

import java.util.List;

public class PushTxBlockCypherResponse {


    public TxBean tx;

    public static class TxBean {
        /**
         * block_height : -1
         * block_index : -1
         * hash : 3f9945890ffed2721b3a831a83b4535cc431b674908d3a89f88fb5b0db15c0b9
         * addresses : ["mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq","2MuSE8MjWWTHguz56RaMfagCssrZrKhcb3Z","myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH"]
         * total : 172993430
         * fees : 6570
         * size : 371
         * preference : high
         * relayed_by : 111.198.124.5
         * received : 2019-01-09T03:18:44.070050361Z
         * ver : 2
         * double_spend : false
         * vin_sz : 2
         * vout_sz : 2
         * confirmations : 0
         * inputs : [{"prev_hash":"644599bbe22ecd608e0c53173edc9fcb00a13847eb1f06b8e33f1937260a3ef2","output_index":0,"script":"47304402201100a2d98794f94bd202d89fc7cfbf23cf7e70a0b50c10eaddb3b0639f70a2d60220353ad413ac7f9fe22023bf71802bab00e05167208437bdada2ef32359f88e04f012102f44a4f8e60ff09e30a2a4a842c8b70555b0b7d9b4c954a2b8f661a5439a4e330","sequence":4294967295,"addresses":["mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq"],"script_type":"pay-to-pubkey-hash","age":1451415},{"prev_hash":"1723394ed6083206dbe4bb0967b98d39b93c27d3c1697b69400cf0a8bf471507","output_index":0,"script":"483045022100d62333ad1d4dc71586f23b8e173e5e7a240c3a70a9cd7483831cc449b6dadc650220490ce777e81565e0de352490c49f6e5daa65221cb875bba2f869150becb87423012102f44a4f8e60ff09e30a2a4a842c8b70555b0b7d9b4c954a2b8f661a5439a4e330","output_value":173000000,"sequence":4294967295,"addresses":["mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq"],"script_type":"pay-to-pubkey-hash","age":1451425}]
         * outputs : [{"value":20000000,"script":"a9141804f17fc7d77309e0554477dda80172506e778b87","addresses":["2MuSE8MjWWTHguz56RaMfagCssrZrKhcb3Z"],"script_type":"pay-to-script-hash"},{"value":152993430,"script":"76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac","addresses":["myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH"],"script_type":"pay-to-pubkey-hash"}]
         */

        public int block_height;
        public int block_index;
        public String hash;
        public int total;
        public int fees;
        public int size;
        public String preference;
        public String relayed_by;
        public String received;
        public int ver;
        public boolean double_spend;
        public int vin_sz;
        public int vout_sz;
        public int confirmations;
        public List<String> addresses;
        public List<InputsBean> inputs;
        public List<OutputsBean> outputs;

        public static class InputsBean {
            /**
             * prev_hash : 644599bbe22ecd608e0c53173edc9fcb00a13847eb1f06b8e33f1937260a3ef2
             * output_index : 0
             * script : 47304402201100a2d98794f94bd202d89fc7cfbf23cf7e70a0b50c10eaddb3b0639f70a2d60220353ad413ac7f9fe22023bf71802bab00e05167208437bdada2ef32359f88e04f012102f44a4f8e60ff09e30a2a4a842c8b70555b0b7d9b4c954a2b8f661a5439a4e330
             * sequence : 4294967295
             * addresses : ["mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq"]
             * script_type : pay-to-pubkey-hash
             * age : 1451415
             * output_value : 173000000
             */

            public String prev_hash;
            public int output_index;
            public String script;
            public long sequence;
            public String script_type;
            public int age;
            public int output_value;
            public List<String> addresses;
        }

        public static class OutputsBean {
            /**
             * value : 20000000
             * script : a9141804f17fc7d77309e0554477dda80172506e778b87
             * addresses : ["2MuSE8MjWWTHguz56RaMfagCssrZrKhcb3Z"]
             * script_type : pay-to-script-hash
             */

            public int value;
            public String script;
            public String script_type;
            public List<String> addresses;
        }
    }
}

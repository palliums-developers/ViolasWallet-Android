package com.violas.wallet.repository.http.btcBrowser.bean;


import com.violas.wallet.repository.http.btcBrowser.request.BTCRequest;
import com.violas.wallet.repository.http.btcBrowser.request.BTCTestRequest;
import com.violas.wallet.repository.http.btcBrowser.request.BTrusteeRequest;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class TransactionBean {

    public String blockhash;
    public long blocktime;
    public long confirmations;
    public String hash;
    public String hex;
    public long locktime;
    public long size;
    public long time;
    public String txid;
    public int version;
    public long vsize;
    public long weight;
    private List<VinBean> vin;
    private List<VoutBean> vout;

    /**
     * blockhash : 000000000000017dd96c640c7ee189b167e3748d97180eef095df6ac319fe95a
     * blocktime : 1551326050
     * confirmations : 2280
     * hash : 674ab5b9f14b995b4da24c6ab95958fcd26ee58b6ba60876e408e391059dd7ef
     * hex : 0200000001eecdac247b699d18b41f0008c8a94af84dfab2861c880844b8dde2c894307b81000000006b483045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6012103e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13fffffffff02a08601000000000017a9144dd8fc50d9b06924364906d860d3ee9aa70c5c0687507cb500000000001976a9145c1692fa77b2904c0bccfc7d9835b2099cb8f17188ac00000000
     * locktime : 0
     * size : 224
     * time : 1551326050
     * txid : 674ab5b9f14b995b4da24c6ab95958fcd26ee58b6ba60876e408e391059dd7ef
     * version : 2
     * vin : [{"scriptSig":{"asm":"3045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6[ALL] 03e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f","hex":"483045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6012103e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f"},"sequence":4294967295,"txid":"817b3094c8e2ddb84408881c86b2fa4df84aa9c808001fb4189d697b24accdee","vout":0}]
     * vout : [{"n":0,"scriptPubKey":{"addresses":["2MzLqxUkbVJxzSkmFAXHKUm7EfQS7pZFvT2"],"asm":"OP_HASH160 4dd8fc50d9b06924364906d860d3ee9aa70c5c06 OP_EQUAL","hex":"a9144dd8fc50d9b06924364906d860d3ee9aa70c5c0687","reqSigs":1,"type":"scripthash"},"value":0.001},{"n":1,"scriptPubKey":{"addresses":["mousWBSN7Rsqi8qpmZp7C6VmRkBGPD5bFF"],"asm":"OP_DUP OP_HASH160 5c1692fa77b2904c0bccfc7d9835b2099cb8f171 OP_EQUALVERIFY OP_CHECKSIG","hex":"76a9145c1692fa77b2904c0bccfc7d9835b2099cb8f17188ac","reqSigs":1,"type":"pubkeyhash"},"value":0.1189384}]
     * vsize : 224
     * weight : 896
     */


    public TransactionBean() {

    }

    public TransactionBean(BTrusteeRequest.BtTranceBean btTranceBean) {
        BTrusteeRequest.BtTranceBean.ResultBean btTrance = btTranceBean.result;
        this.blockhash = btTrance.blockhash;
        this.blocktime = btTrance.blocktime;
        this.confirmations = btTrance.confirmations;
        this.hash = btTrance.hash;
        this.hex = btTrance.hex;
        this.locktime = btTrance.locktime;
        this.version = btTrance.version;

        List<BTrusteeRequest.BtTranceBean.ResultBean.VoutBean> list = btTrance.vout;

        List<VoutBean> data = new LinkedList<>();
        VoutBean bean;
        for (BTrusteeRequest.BtTranceBean.ResultBean.VoutBean voutBean : list) {
            bean = new VoutBean();
            bean.setN(voutBean.n);
            bean.setValue(voutBean.value);

            VoutBean.ScriptPubKeyBean scriptPubKeyBean = new VoutBean.ScriptPubKeyBean();
            scriptPubKeyBean.setAddresses(voutBean.scriptPubKey.addresses);
            scriptPubKeyBean.setAsm(voutBean.scriptPubKey.asm);
            scriptPubKeyBean.setHex(voutBean.scriptPubKey.hex);
            scriptPubKeyBean.setType(voutBean.scriptPubKey.type);
            bean.setScriptPubKey(scriptPubKeyBean);
            data.add(bean);
        }
        setVout(data);
    }

    public TransactionBean(String blockhash, long blocktime, int confirmations, String hash, String hex, long locktime, int version) {
        this.blockhash = blockhash;
        this.blocktime = blocktime;
        this.confirmations = confirmations;
        this.hash = hash;
        this.hex = hex;
        this.locktime = locktime;
        this.version = version;
    }

    public TransactionBean(BTCRequest.TranceBean.DataBean btTrance) {
        this.blockhash = btTrance.block_hash;
        this.blocktime = btTrance.block_time;
        this.confirmations = btTrance.confirmations;
        this.hash = btTrance.hash;
        this.hex = btTrance.hash;
        this.locktime = btTrance.lock_time;
        this.version = btTrance.version;

        List<BTCRequest.TranceBean.DataBean.OutputsBean> list = btTrance.outputs;

        List<VoutBean> data = new LinkedList<>();
        VoutBean bean;
        int i = 0;
        for (BTCRequest.TranceBean.DataBean.OutputsBean voutBean : list) {
            bean = new VoutBean();
            bean.setN(i);
            bean.setValue(new BigDecimal(voutBean.value + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP).doubleValue());

            VoutBean.ScriptPubKeyBean scriptPubKeyBean = new VoutBean.ScriptPubKeyBean();
            scriptPubKeyBean.setAddresses(voutBean.addresses);
            scriptPubKeyBean.setAsm(voutBean.script_asm);
            scriptPubKeyBean.setHex(voutBean.script_hex);
            scriptPubKeyBean.setType(voutBean.type);
            bean.setScriptPubKey(scriptPubKeyBean);
            data.add(bean);
            i++;
        }
        setVout(data);
    }

    public TransactionBean(BTCTestRequest.TranceBean.DataBean btTrance) {
        this.blockhash = btTrance.block_hash;
        this.blocktime = btTrance.block_time;
        this.confirmations = btTrance.confirmations;
        this.hash = btTrance.hash;
        this.hex = btTrance.hash;
        this.locktime = btTrance.lock_time;
        this.version = btTrance.version;

        List<BTCTestRequest.TranceBean.DataBean.OutputsBean> list = btTrance.outputs;

        List<VoutBean> data = new LinkedList<>();
        VoutBean bean;
        int i = 0;
        for (BTCTestRequest.TranceBean.DataBean.OutputsBean voutBean : list) {
            bean = new VoutBean();
            bean.setN(i);
            bean.setValue(new BigDecimal(voutBean.value + "").divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP).doubleValue());

            VoutBean.ScriptPubKeyBean scriptPubKeyBean = new VoutBean.ScriptPubKeyBean();
            scriptPubKeyBean.setAddresses(voutBean.addresses);
            scriptPubKeyBean.setAsm(voutBean.script_asm);
            scriptPubKeyBean.setHex(voutBean.script_hex);
            scriptPubKeyBean.setType(voutBean.type);
            bean.setScriptPubKey(scriptPubKeyBean);
            data.add(bean);
            i++;
        }
        setVout(data);
    }

    public String getBlockhash() {
        return blockhash;
    }

    public void setBlockhash(String blockhash) {
        this.blockhash = blockhash;
    }

    public long getBlocktime() {
        return blocktime;
    }

    public void setBlocktime(long blocktime) {
        this.blocktime = blocktime;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(long confirmations) {
        this.confirmations = confirmations;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHex() {
        return hex;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }

    public long getLocktime() {
        return locktime;
    }

    public void setLocktime(long locktime) {
        this.locktime = locktime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getVsize() {
        return vsize;
    }

    public void setVsize(long vsize) {
        this.vsize = vsize;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public List<VinBean> getVin() {
        return vin;
    }

    public void setVin(List<VinBean> vin) {
        this.vin = vin;
    }

    public List<VoutBean> getVout() {
        return vout;
    }

    public void setVout(List<VoutBean> vout) {
        this.vout = vout;
    }

    public static class VinBean {
        /**
         * scriptSig : {"asm":"3045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6[ALL] 03e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f","hex":"483045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6012103e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f"}
         * sequence : 4294967295
         * txid : 817b3094c8e2ddb84408881c86b2fa4df84aa9c808001fb4189d697b24accdee
         * vout : 0
         */

        private ScriptSigBean scriptSig;
        private long sequence;
        private String txid;
        private int vout;

        public ScriptSigBean getScriptSig() {
            return scriptSig;
        }

        public void setScriptSig(ScriptSigBean scriptSig) {
            this.scriptSig = scriptSig;
        }

        public long getSequence() {
            return sequence;
        }

        public void setSequence(long sequence) {
            this.sequence = sequence;
        }

        public String getTxid() {
            return txid;
        }

        public void setTxid(String txid) {
            this.txid = txid;
        }

        public int getVout() {
            return vout;
        }

        public void setVout(int vout) {
            this.vout = vout;
        }

        public static class ScriptSigBean {
            /**
             * asm : 3045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6[ALL] 03e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f
             * hex : 483045022100c229c8ee9dec4e4ad6593cd5f499e15dac7d1c97fc34773ed7d41fb5cfd5464802202699dd102d428f8c4451850f52d3400bf9316856d43ef38cc4af9b01d36490f6012103e4269ced8bcbfc070abbc4b91f07a9535aaadde7c4e24a3b7071510b7f7fe13f
             */

            private String asm;
            private String hex;

            public String getAsm() {
                return asm;
            }

            public void setAsm(String asm) {
                this.asm = asm;
            }

            public String getHex() {
                return hex;
            }

            public void setHex(String hex) {
                this.hex = hex;
            }
        }
    }

    public static class VoutBean {
        /**
         * n : 0
         * scriptPubKey : {"addresses":["2MzLqxUkbVJxzSkmFAXHKUm7EfQS7pZFvT2"],"asm":"OP_HASH160 4dd8fc50d9b06924364906d860d3ee9aa70c5c06 OP_EQUAL","hex":"a9144dd8fc50d9b06924364906d860d3ee9aa70c5c0687","reqSigs":1,"type":"scripthash"}
         * value : 0.001
         */

        private int n;
        private ScriptPubKeyBean scriptPubKey;
        private double value;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof VoutBean)) return false;
            VoutBean other = (VoutBean) obj;
            return n == other.getN();
        }

        public int getN() {
            return n;
        }

        public void setN(int n) {
            this.n = n;
        }

        public ScriptPubKeyBean getScriptPubKey() {
            return scriptPubKey;
        }

        public void setScriptPubKey(ScriptPubKeyBean scriptPubKey) {
            this.scriptPubKey = scriptPubKey;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public static class ScriptPubKeyBean {
            /**
             * addresses : ["2MzLqxUkbVJxzSkmFAXHKUm7EfQS7pZFvT2"]
             * asm : OP_HASH160 4dd8fc50d9b06924364906d860d3ee9aa70c5c06 OP_EQUAL
             * hex : a9144dd8fc50d9b06924364906d860d3ee9aa70c5c0687
             * reqSigs : 1
             * type : scripthash
             */

            private String asm;
            private String hex;
            private int reqSigs;
            private String type;
            private List<String> addresses;

            public String getAsm() {
                return asm;
            }

            public void setAsm(String asm) {
                this.asm = asm;
            }

            public String getHex() {
                return hex;
            }

            public void setHex(String hex) {
                this.hex = hex;
            }

            public int getReqSigs() {
                return reqSigs;
            }

            public void setReqSigs(int reqSigs) {
                this.reqSigs = reqSigs;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public List<String> getAddresses() {
                return addresses;
            }

            public void setAddresses(List<String> addresses) {
                this.addresses = addresses;
            }
        }
    }
}

package com.violas.wallet.repository.http.bitcoinChainApi.bean;


import androidx.annotation.IntDef;

import com.quincysx.crypto.bitcoin.script.Script;
import com.quincysx.crypto.utils.HexUtils;

public class UTXO {
    /**
     * P2PKH
     */
    public static final int NONE = -1;
    /**
     * P2PKH
     */
    public static final int P2PKH = 0;
    /**
     * P2SH
     */
    public static final int P2SH = 1;
    /**
     * 定期合同
     */
    public static final int DEPOSIT = 2;
    /**
     * 活期合同
     */
    public static final int DEMAND_DEPOSIT = 3;

    @IntDef({
            NONE, P2PKH, P2SH, DEPOSIT, DEMAND_DEPOSIT
    })
    public @interface UTXOType {
    }

    /**
     * address : myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH
     * txid : 9bf38f88faa4cd730ea29ef5df5b1a5a4baa3e59c1d044a56abe9c28a1764e25
     * vout : 1
     * scriptPubKey : 76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac
     * amount : 0.16934404
     * height : 1444055
     * confirmations : 604
     */

    private String address;
    private String txid;
    private int vout;
    private String scriptPubKey;
    private double amount;
    private int height;
    private long confirmations;

    public UTXO() {
    }

    public UTXO(String address, String txid, int vout, String scriptPubKey, double amount, int height, long confirmations) {
        this.address = address;
        this.txid = txid;
        this.vout = vout;
        this.scriptPubKey = scriptPubKey;
        this.amount = amount;
        this.height = height;
        this.confirmations = confirmations;
    }

    @UTXOType
    public int getUtxoType() {
        byte[] script = HexUtils.fromHex(scriptPubKey);
        if (script[0] == Script.OP_DUP && script[1] == Script.OP_HASH160) {
            return P2PKH;
        } else if (script[0] == Script.OP_HASH160) {
            return P2SH;
        }
        return NONE;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getScriptPubKey() {
        return scriptPubKey;
    }

    public void setScriptPubKey(String scriptPubKey) {
        this.scriptPubKey = scriptPubKey;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(long confirmations) {
        this.confirmations = confirmations;
    }
}

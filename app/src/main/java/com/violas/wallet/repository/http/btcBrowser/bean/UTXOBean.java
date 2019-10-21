package com.violas.wallet.repository.http.btcBrowser.bean;

public class UTXOBean {

    /**
     * address : myj2WhvzShjzmvr4riRTxYDnxRvg6UwCVH
     * txid : 9bf38f88faa4cd730ea29ef5df5b1a5a4baa3e59c1d044a56abe9c28a1764e25
     * vout : 1
     * scriptPubKey : 76a914c7bac6db1cc9a1be72e8b058d2c59f6127099aeb88ac
     * amount : 0.16934404
     * satoshis : 16934404
     * height : 1444055
     * confirmations : 604
     */

    private String address;
    private String txid;
    private int vout;
    private String scriptPubKey;
    private double amount;
    private int satoshis;
    private int height;
    private int confirmations;

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

    public int getSatoshis() {
        return satoshis;
    }

    public void setSatoshis(int satoshis) {
        this.satoshis = satoshis;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }
}

package com.violas.wallet.repository.http.bitcoinChainApi.bean;

public class UTXOBlockCypher {

    /**
     * tx_hash : 1723394ed6083206dbe4bb0967b98d39b93c27d3c1697b69400cf0a8bf471507
     * block_height : 1451425
     * tx_input_n : -1
     * tx_output_n : 0
     * value : 173000000
     * ref_balance : 183000000
     * spent : false
     * confirmations : 5
     * confirmed : 2019-01-08T06:50:23Z
     * double_spend : false
     * script : 76a9145dd9d90397dbcc62418727c320b00c825c98339688ac
     */

    private String tx_hash;
    private int block_height;
    private int tx_input_n;
    private int tx_output_n;
    private int value;
    private int ref_balance;
    private boolean spent;
    private int confirmations;
    private String confirmed;
    private boolean double_spend;
    private String script;

    public String getTx_hash() {
        return tx_hash;
    }

    public void setTx_hash(String tx_hash) {
        this.tx_hash = tx_hash;
    }

    public int getBlock_height() {
        return block_height;
    }

    public void setBlock_height(int block_height) {
        this.block_height = block_height;
    }

    public int getTx_input_n() {
        return tx_input_n;
    }

    public void setTx_input_n(int tx_input_n) {
        this.tx_input_n = tx_input_n;
    }

    public int getTx_output_n() {
        return tx_output_n;
    }

    public void setTx_output_n(int tx_output_n) {
        this.tx_output_n = tx_output_n;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getRef_balance() {
        return ref_balance;
    }

    public void setRef_balance(int ref_balance) {
        this.ref_balance = ref_balance;
    }

    public boolean isSpent() {
        return spent;
    }

    public void setSpent(boolean spent) {
        this.spent = spent;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public String getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(String confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isDouble_spend() {
        return double_spend;
    }

    public void setDouble_spend(boolean double_spend) {
        this.double_spend = double_spend;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}

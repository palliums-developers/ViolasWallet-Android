package com.violas.wallet.repository.http.btcBrowser.respones;

public class BalanceBlockCypherResponse {

    /**
     * address : mp5C7DdXwXfEaAYhncnoxgKPaka3rGG4yq
     * total_received : 183000000
     * total_sent : 0
     * balance : 183000000
     * unconfirmed_balance : 0
     * final_balance : 183000000
     * n_tx : 3
     * unconfirmed_n_tx : 0
     * final_n_tx : 3
     */

    private String address;
    private int total_received;
    private int total_sent;
    private int balance;
    private int unconfirmed_balance;
    private int final_balance;
    private int n_tx;
    private int unconfirmed_n_tx;
    private int final_n_tx;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTotal_received() {
        return total_received;
    }

    public void setTotal_received(int total_received) {
        this.total_received = total_received;
    }

    public int getTotal_sent() {
        return total_sent;
    }

    public void setTotal_sent(int total_sent) {
        this.total_sent = total_sent;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getUnconfirmed_balance() {
        return unconfirmed_balance;
    }

    public void setUnconfirmed_balance(int unconfirmed_balance) {
        this.unconfirmed_balance = unconfirmed_balance;
    }

    public int getFinal_balance() {
        return final_balance;
    }

    public void setFinal_balance(int final_balance) {
        this.final_balance = final_balance;
    }

    public int getN_tx() {
        return n_tx;
    }

    public void setN_tx(int n_tx) {
        this.n_tx = n_tx;
    }

    public int getUnconfirmed_n_tx() {
        return unconfirmed_n_tx;
    }

    public void setUnconfirmed_n_tx(int unconfirmed_n_tx) {
        this.unconfirmed_n_tx = unconfirmed_n_tx;
    }

    public int getFinal_n_tx() {
        return final_n_tx;
    }

    public void setFinal_n_tx(int final_n_tx) {
        this.final_n_tx = final_n_tx;
    }
}

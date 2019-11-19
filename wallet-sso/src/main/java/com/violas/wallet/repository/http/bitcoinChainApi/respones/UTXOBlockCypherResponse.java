package com.violas.wallet.repository.http.bitcoinChainApi.respones;


import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXOBlockCypher;

import java.util.List;

public class UTXOBlockCypherResponse {

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

    public  String address;
    public int total_received;
    public int total_sent;
    public int balance;
    public int unconfirmed_balance;
    public int final_balance;
    public int n_tx;
    public int unconfirmed_n_tx;
    public int final_n_tx;
    public List<UTXOBlockCypher> txrefs;
}

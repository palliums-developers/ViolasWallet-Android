package com.quincysx.crypto;

import com.quincysx.crypto.exception.CoinNotFindException;

/**
 * Created by q7728 on 2018/3/18.
 */

public enum CoinTypes {
    Bitcoin(0, "BTC"),
    BitcoinTest(1, "BTC"),
    VToken(-2, "VToken"),
    Libra(-1, "Libra");

    private int coinType;
    private String coinName;

    CoinTypes(int i, String name) {
        coinType = i;
        coinName = name;
    }

    public int coinType() {
        return coinType;
    }

    public String coinName() {
        return coinName;
    }

    public static CoinTypes parseCoinType(int type) throws CoinNotFindException {
        for (CoinTypes e : CoinTypes.values()) {
            if (e.coinType == type) {
                return e;
            }
        }
        throw new CoinNotFindException("The currency is not supported for the time being");
    }
}

package com.quincysx.crypto;

import com.quincysx.crypto.exception.CoinNotFindException;

/**
 * Created by q7728 on 2018/3/18.
 */

public enum CoinTypes {
    Bitcoin(0, "BTC", "BTC"),
    BitcoinTest(1, "BTC", "BTC"),
    VToken(-2, "VToken", "Vtoken"),
    Libra(-1, "Libra", "Lib");

    private int coinType;
    private String coinName;
    private String coinUnit;

    CoinTypes(int i, String name, String unit) {
        coinType = i;
        coinName = name;
        coinUnit = unit;
    }

    public int coinType() {
        return coinType;
    }

    public String coinName() {
        return coinName;
    }

    public String coinUnit() {
        return coinUnit;
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

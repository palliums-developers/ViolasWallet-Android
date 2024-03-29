package com.quincysx.crypto;

import com.quincysx.crypto.exception.CoinNotFindException;

/**
 * Created by q7728 on 2018/3/18.
 */

public enum CoinTypes {
    Bitcoin(0, "BTC", "Bitcoin", "BTC"),
    BitcoinTest(1, "BTC", "Bitcoin", "BTC"),
    Violas(-2, "vtoken", "Violas", "vtoken"),
    Libra(-1, "libra", "Libra", "libra");

    private int coinType;
    private String coinName;
    private String fullName;
    private String coinUnit;

    CoinTypes(int i, String name, String fullName, String unit) {
        coinType = i;
        coinName = name;
        coinUnit = unit;
        this.fullName = fullName;
    }

    public int coinType() {
        return coinType;
    }

    public String coinName() {
        return coinName;
    }

    public String fullName() {
        return fullName;
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

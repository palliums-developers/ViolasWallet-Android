package com.quincysx.crypto;

import com.quincysx.crypto.exception.CoinNotFindException;

/**
 * Created by q7728 on 2018/3/18.
 */

public enum CoinType {
    Bitcoin(0, "BTC", "BTC", "Bitcoin"),
    BitcoinTest(1, "BTC", "BTC", "Bitcoin"),
    Violas(-4, "VLS", "VLS", "Violas"),
    ViolasTest(-2, "VLS", "VLS", "Violas"),
    Diem(-3, "LBR", "LBR", "Libra"),
    DiemTest(-1, "LBR", "LBR", "Libra");

    private final int coinNumber;
    private final String coinName;
    private final String coinUnit;
    private final String chainName;

    CoinType(int coinNumber, String coinName, String coinUnit, String chainName) {
        this.coinNumber = coinNumber;
        this.coinName = coinName;
        this.coinUnit = coinUnit;
        this.chainName = chainName;
    }

    public int coinNumber() {
        return coinNumber;
    }

    public String coinName() {
        return coinName;
    }

    public String coinUnit() {
        return coinUnit;
    }

    public String chainName() {
        return chainName;
    }

    public static CoinType parseCoinNumber(int coinNumber) throws CoinNotFindException {
        for (CoinType coinType : CoinType.values()) {
            if (coinType.coinNumber == coinNumber) {
                return coinType;
            }
        }
        throw new CoinNotFindException("The currency is not supported for the time being");
    }
}

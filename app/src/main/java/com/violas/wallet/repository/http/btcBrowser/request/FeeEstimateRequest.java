package com.violas.wallet.repository.http.btcBrowser.request;

import io.reactivex.Observable;

public class FeeEstimateRequest {

    public Observable<FeesBean> estimateFee() {
//        return new Fee21Request().getFee();
        return new FeeBlockcypherRequest().estimateFee();
    }

    public static class FeesBean {

        /**
         * fastestFee : 12
         * halfHourFee : 10
         * hourFee : 2
         */

        public int fastestFee;
        public int halfHourFee;
        public int hourFee;
    }
}

package com.violas.wallet.repository.http.bitcoinChainApi.request;

import io.reactivex.Observable;

public class FeeEstimateRequest {

    public Observable<FeesBean> estimateFee() {
//        return new Fee21Request().getFee();
        return new FeeBlockcypherRequest().estimateFee();
    }

    public static class FeesBean {
        public FeesBean(long fastestFee, long halfHourFee, long hourFee) {
            this.fastestFee = fastestFee;
            this.halfHourFee = halfHourFee;
            this.hourFee = hourFee;
        }

        /**
         * fastestFee : 12
         * halfHourFee : 10
         * hourFee : 2
         */
        public long fastestFee;
        public long halfHourFee;
        public long hourFee;
    }
}

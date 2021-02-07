package com.violas.wallet.repository.http.bitcoinChainApi.request;

import com.violas.wallet.common.VmHelperKt;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class FeeBlockcypherRequest extends BaseRequest<FeeBlockcypherRequest.Api> {
    @Override
    public String requestUrl() {
        return "https://api.blockcypher.com/";
    }

    @Override
    protected Class requestApi() {
        return Api.class;
    }

    public interface Api {
        @GET("v1/btc/{network}")
        Observable<FeesDTO> getFees(@Path("network") String str);
    }

    public static class FeesDTO {

        /**
         * name : BTC.test3
         * height : 1489160
         * hash : 000000000000003e64179e1780777eddbf9b2de87ad862e73d9d84f736d8f023
         * time : 2019-04-15T09:25:49.892214998Z
         * latest_url : https://api.blockcypher.com/v1/btc/test3/blocks/000000000000003e64179e1780777eddbf9b2de87ad862e73d9d84f736d8f023
         * previous_hash : 0000000044c0fe8b4ee267306099fc81f7bf72c33cedc1b4b84ba5d3277b34e3
         * previous_url : https://api.blockcypher.com/v1/btc/test3/blocks/0000000044c0fe8b4ee267306099fc81f7bf72c33cedc1b4b84ba5d3277b34e3
         * peer_count : 284
         * unconfirmed_count : 71
         * high_fee_per_kb : 13691
         * medium_fee_per_kb : 10000
         * low_fee_per_kb : 5000
         * last_fork_height : 1488663
         * last_fork_hash : 00000000678b725749b7bd6f60182abc1e6f34dbe3daa57ebab738724e7e726d
         */

        public String name;
        public long height;
        public String hash;
        public String time;
        public String latest_url;
        public String previous_hash;
        public String previous_url;
        public long peer_count;
        public long unconfirmed_count;
        public long high_fee_per_kb;
        public long medium_fee_per_kb;
        public long low_fee_per_kb;
        public int last_fork_height;
        public String last_fork_hash;
    }

    public Observable<FeeEstimateRequest.FeesBean> estimateFee() {
        String network;
        if (VmHelperKt.isBitcoinTestNet()) {
            network = "test3";
        } else {
            network = "main";
        }
        return getRequest().getFees(network)
                .map(new Function<FeesDTO, FeeEstimateRequest.FeesBean>() {
                    @Override
                    public FeeEstimateRequest.FeesBean apply(FeesDTO feesBean) throws Exception {
                        FeeEstimateRequest.FeesBean bean = new FeeEstimateRequest.FeesBean(
                                feesBean.high_fee_per_kb / 1000,
                                feesBean.medium_fee_per_kb / 1000,
                                feesBean.low_fee_per_kb / 1000
                        );
                        return bean;
                    }
                });
    }
}

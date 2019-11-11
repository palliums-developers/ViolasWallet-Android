package com.violas.wallet.repository.http.bitcoinChainApi.request;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Fee21Request extends BaseRequest<Fee21Request.Api> {

    @Override
    public String requestUrl() {
        return "https://bitcoinfees.earn.com/api/v1/";
    }

    @Override
    protected Class requestApi() {
        return Api.class;
    }

    public interface Api {
        @GET("fees/recommended")
        Observable<FeeEstimateRequest.FeesBean> getFees(@Query("v") String str);
    }

    public Observable<FeeEstimateRequest.FeesBean> getFee() {
        return getRequest().getFees("1");
    }
}

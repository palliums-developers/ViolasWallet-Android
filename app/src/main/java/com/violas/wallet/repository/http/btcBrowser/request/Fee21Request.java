package com.violas.wallet.repository.http.btcBrowser.request;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Fee21Request extends BaseRequest {
    private static Api api;

    static {
        api = sRetrofit.create(Api.class);
    }

    public interface Api {
        @GET("https://bitcoinfees.earn.com/api/v1/fees/recommended")
        Observable<FeeEstimateRequest.FeesBean> getFees(@Query("v") String str);
    }

    public Observable<FeeEstimateRequest.FeesBean> getFee() {
        return api.getFees("1");
    }
}

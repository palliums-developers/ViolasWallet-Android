package com.violas.wallet.repository.http.bitcoinChainApi.request;


import com.violas.wallet.BuildConfig;
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class BaseRequest<T> {

    private T request;

    public abstract String requestUrl();

    protected abstract Class<T> requestApi();

    public T getRequest() {
        if (request == null) {
            synchronized (BaseRequest.class) {
                if (request == null) {
                    request = getRetrofit().create(requestApi());
                }
            }
        }
        return request;
    }

    private Retrofit getRetrofit() {
        final HttpLoggingInterceptor.Level logLevel;
        if (BuildConfig.DEBUG) {
            logLevel = HttpLoggingInterceptor.Level.BODY;
        } else {
            logLevel = HttpLoggingInterceptor.Level.NONE;
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .callTimeout(100, TimeUnit.SECONDS)
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
//                .addInterceptor(sChainInterceptor)
                .addInterceptor(new RequestHeaderInterceptor())
                .addInterceptor(new HttpLoggingInterceptor().setLevel(logLevel))
                .build();

        return new Retrofit.Builder()
                .baseUrl(requestUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}

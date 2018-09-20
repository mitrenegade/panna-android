package io.renderapps.balizinha.module;

/**
 * Created by joel on 1/3/18.
 */


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory to generate our Retrofit instance.
 */
public class RetrofitFactory {

    // Base URL
    private static final String BASE_URL_PROD = "https://us-central1-balizinha-c9cd7.cloudfunctions.net/";
    private static final String BASE_URL_DEV = "https://us-central1-balizinha-dev.cloudfunctions.net/";
    private static Retrofit mInstance = null;

    public static Retrofit getInstance() {
        if (mInstance == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

            // log level- Level.BODY for debugging errors.
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS);

            httpClient.addInterceptor(logging);

            // Adding Rx so the calls can be Observable, and adding a Gson converter with
            // leniency to make parsing the results simple.
            mInstance = new Retrofit.Builder()
                    .baseUrl(BASE_URL_DEV)
//                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return mInstance;
    }
}
package io.renderapps.balizinha.module;

/**
 * Created by joel on 1/3/18.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
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
            // Set your desired log level. Use Level.BODY for debugging errors.
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Adding Rx so the calls can be Observable, and adding a Gson converter with
            // leniency to make parsing the results simple.
            mInstance = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .baseUrl(BASE_URL_PROD)
                    .client(httpClient.build())
                    .build();
        }
        return mInstance;
    }
}
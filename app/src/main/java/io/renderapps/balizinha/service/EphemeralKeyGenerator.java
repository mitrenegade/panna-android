package io.renderapps.balizinha.service;

import android.support.annotation.NonNull;

import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.EphemeralKeyUpdateListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import io.renderapps.balizinha.module.RetrofitFactory;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

/**
 * Generates ephemeral key required to create customer session with Stripe
 */

public class EphemeralKeyGenerator implements EphemeralKeyProvider {

    private @NonNull
    ProgressListener mProgressListener;
    private @NonNull
    CompositeDisposable mCompositeSubscription;
    private @NonNull StripeService mStripeService;
    private String customerId;


    public EphemeralKeyGenerator(@NonNull ProgressListener progressListener, String customerId) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);
        mCompositeSubscription = new CompositeDisposable();
        mProgressListener = progressListener;
        this.customerId = customerId;
    }


    @Override
    public void createEphemeralKey(@NonNull String apiVersion,
                                   @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
        Map<String, String> apiParamMap = new HashMap<>();
        apiParamMap.put("api_version", apiVersion);
        apiParamMap.put("customer_id", customerId);
        mCompositeSubscription.add(
                mStripeService.createEphemeralKey(apiParamMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<ResponseBody>() {
                            @Override
                            public void onComplete() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                mProgressListener.onStringResponse(e.getMessage());
                            }

                            @Override
                            public void onNext(ResponseBody response) {
                                try {
                                    String rawKey = response.string();
                                    keyUpdateListener.onKeyUpdate(rawKey);
                                    mProgressListener.onStringResponse(rawKey);
                                } catch (IOException iox) { }
                            }
                        }));
    }

    public interface ProgressListener {
        void onStringResponse(String string);
    }
}

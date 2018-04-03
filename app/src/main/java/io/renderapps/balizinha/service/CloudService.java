package io.renderapps.balizinha.service;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.renderapps.balizinha.module.RetrofitFactory;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Helper class calls several cloud functions that work with Firebase to
 * write and read data
 */

public class CloudService {

    public interface ProgressListener {
        void onStringResponse(String string);
    }

    private @NonNull
    ProgressListener mProgressListener;
    private @NonNull
    CompositeSubscription mCompositeSubscription;
    private @NonNull StripeService mStripeService;
    private Retrofit retrofit;

    public CloudService(@NonNull ProgressListener progressListener) {
        retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);
        mCompositeSubscription = new CompositeSubscription();
        mProgressListener = progressListener;
    }

    /**
     * Returns users stripe customer id if exists, else will create one and then return it
     * @param userId users unique firebase id
     * @param email users email address registered with firebase
     */
    public void validateStripeCustomer(String userId, String email){
        Map<String, String> customerMap = new HashMap<>();
        customerMap.put("userId", userId);
        customerMap.put("email", email);

        mCompositeSubscription.add(mStripeService.validateStripeCustomer(customerMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {}
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));


    }
}

package io.renderapps.balizinha.service;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import io.reactivex.Observable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.renderapps.balizinha.module.RetrofitFactory;
import io.renderapps.balizinha.service.stripe.StripeService;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

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
    private Retrofit retrofit;

    public CloudService(@NonNull ProgressListener progressListener) {
        retrofit = RetrofitFactory.getInstance();
        mProgressListener = progressListener;
    }

    /**
     * Returns users stripe customer id if exists, else will create one and then return it
     * @param userId users unique firebase id
     * @param email users email address registered with firebase
     */
    public void validateStripeCustomer(String userId, String email){
        final StripeService mStripeService = retrofit.create(StripeService.class);

        Map<String, String> customerMap = new HashMap<>();
        customerMap.put("userId", userId);
        customerMap.put("email", email);

        new CompositeDisposable().add(mStripeService.validateStripeCustomer(customerMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                }));

    }

    public void holdPaymentForEvent(@NonNull String userId, @NonNull String eventId){
        final StripeService mStripeService = retrofit.create(StripeService.class);

        new CompositeDisposable().add(mStripeService.holdPaymentForEvent(userId, eventId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                }));
    }


    public void getLeaguesForPlayer(String userId){
        final LeagueService leagueService = retrofit.create(LeagueService.class);

        new CompositeDisposable().add(leagueService.getLeaguesForPlayer(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                }));
    }

    public void changeLeaguePlayerStatus(String userId, String leagueId, String status){
        final LeagueService leagueService = retrofit.create(LeagueService.class);

        new CompositeDisposable().add(leagueService.changeLeaguePlayerStatus(userId, leagueId, status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                }));
    }

    public void createEvent(HashMap<String, Object> params){
        final EventApiService eventService = retrofit.create(EventApiService.class);

        new CompositeDisposable().add(eventService.createEvent(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                }));
    }

    public void getAvailableEvents(String userId){
        final EventApiService eventService = retrofit.create(EventApiService.class);
        call(eventService.getEventsAvailableToUser(userId));
    }


    private void call(Observable<ResponseBody> observable){
        new CompositeDisposable().add(observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressListener.onStringResponse(e.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                }));
    }
}

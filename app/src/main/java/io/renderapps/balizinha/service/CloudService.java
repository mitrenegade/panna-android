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

        new CompositeSubscription().add(mStripeService.validateStripeCustomer(customerMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String rawKey = response.string();
                            mProgressListener.onStringResponse(rawKey);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));


    }

    public void holdPaymentForEvent(@NonNull String userId, @NonNull String eventId){
        final StripeService mStripeService = retrofit.create(StripeService.class);

        new CompositeSubscription().add(mStripeService.holdPaymentForEvent(userId, eventId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String jsonResponse = response.string();
                            mProgressListener.onStringResponse(jsonResponse);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));
    }


    public void getLeaguesForPlayer(String userId){
        final LeagueService leagueService = retrofit.create(LeagueService.class);

        new CompositeSubscription().add(leagueService.getLeaguesForPlayer(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String jsonResponse = response.string();
                            mProgressListener.onStringResponse(jsonResponse);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));
    }

    public void getLeaguePlayers(String leagueId){
        final LeagueService leagueService = retrofit.create(LeagueService.class);
        new CompositeSubscription().add(leagueService.getPlayersForLeague(leagueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String jsonResponse = response.string();
                            mProgressListener.onStringResponse(jsonResponse);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));
    }

    public void getLeagueEvents(String leagueId){
        final LeagueService leagueService = retrofit.create(LeagueService.class);
        new CompositeSubscription().add(leagueService.getEventsForLeague(leagueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String jsonResponse = response.string();
                            mProgressListener.onStringResponse(jsonResponse);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));
    }

    public void changeLeaguePlayerStatus(String userId, String leagueId, String status){
        final LeagueService leagueService = retrofit.create(LeagueService.class);

        new CompositeSubscription().add(leagueService.changeLeaguePlayerStatus(userId, leagueId, status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody response) {
                        try {
                            String jsonResponse = response.string();
                            mProgressListener.onStringResponse(jsonResponse);
                        } catch (IOException iox) {
                            mProgressListener.onStringResponse("");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mProgressListener.onStringResponse(throwable.getMessage());
                    }
                }));
    }
}

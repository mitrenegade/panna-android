package io.renderapps.balizinha.service;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

/**
 * A Retrofit service used to communicate with a balizinha server.
 */

public interface StripeService {

    @FormUrlEncoded
    @POST("ephemeralKeys")
    Observable<ResponseBody> createEphemeralKey(@FieldMap Map<String, String> apiVersionMap);

//    @FormUrlEncoded
//    @POST("createStripeCustomerForLegacyUser")
//    Observable<ResponseBody> createCustomer(@FieldMap Map<String, String> customerMap);

    @FormUrlEncoded
    @POST("validateStripeCustomer")
    Observable<ResponseBody> validateStripeCustomer(@FieldMap Map<String, String> customerMap);

}
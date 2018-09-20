package io.renderapps.balizinha.service.stripe;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * A Retrofit service used to communicate with a balizinha server.
 */

public interface StripeService {

    @FormUrlEncoded
    @POST("ephemeralKeys")
    Observable<ResponseBody> createEphemeralKey(@FieldMap Map<String, String> apiVersionMap);

    @FormUrlEncoded
    @POST("validateStripeCustomer")
    Observable<ResponseBody> validateStripeCustomer(@FieldMap Map<String, String> customerMap);

    @FormUrlEncoded
    @POST("holdPayment")
    Observable<ResponseBody> holdPaymentForEvent(@Field("userId") String userId,
                                                 @Field("eventId") String eventId);
}
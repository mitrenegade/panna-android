package io.renderapps.balizinha.service;


import java.util.HashMap;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface EventApiService {
    @POST("createEvent")
    Observable<ResponseBody> createEvent(@Body HashMap<String, Object> body);

    @FormUrlEncoded
    @POST("getEventsAvailableToUser")
    Observable<ResponseBody> getEventsAvailableToUser(@Field("userId") String userId);


    @POST("joinOrLeaveEvent")
    Observable<ResponseBody> joinOrLeaveEvent(@Body HashMap<String, Object> body);

}

package io.renderapps.balizinha.service;


import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

public interface LeagueService {

    @FormUrlEncoded
    @POST("getLeaguesForPlayer")
    Observable<ResponseBody> getLeaguesForPlayer(@Field("userId") String userId);

    @FormUrlEncoded
    @POST("getPlayersForLeague")
    Observable<ResponseBody> getPlayersForLeague(@Field("leagueId") String leagueId);

    @FormUrlEncoded
    @POST("getEventsForLeague")
    Observable<ResponseBody> getEventsForLeague(@Field("leagueId") String leagueId);

    @FormUrlEncoded
    @POST("changeLeaguePlayerStatus")
    Observable<ResponseBody> changeLeaguePlayerStatus(@Field("userId") String userId,
                                                      @Field("leagueId") String leagueId, @Field("status") String status);

}

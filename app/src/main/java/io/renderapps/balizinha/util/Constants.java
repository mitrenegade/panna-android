package io.renderapps.balizinha.util;

/**
 * Constants utilized throughout the app(endpoints, permissions, flags etc)
 */

public class Constants {
    public static final String APP_VERSION = "0.3.8";
    public static final boolean IN_DEV_MODE = false;

    // db endpoints
    public static final String REF_ACTIONS = "actions";
    public static final String REF_PLAYERS = "players";
    public static final String REF_EVENTS = "events";
    public static final String REF_EVENT_USERS = "eventUsers";
    public static final String REF_USER_EVENTS = "userEvents";
    public static final String REF_LEAGUES = "leagues";
    public static final String REF_LEAGUE_PLAYERS = "leaguePlayers";
    public static final String REF_PLAYER_LEAGUES = "playerLeagues";
    public static final String REF_CHARGES = "charges";

    // storage
    public static final String REF_STORAGE_IMAGES = "images";
    public static final String REF_STORAGE_EVENT = "event";
    public static final String REF_STORAGE_PLAYER = "player";
    public static final String REF_STORAGE_LEAGUE = "league";

    // remote config
    public static final String CONFIG_PAYMENT_KEY = "paymentRequired";
    public static final String CONFIG_AVAILABLE_EVENTS = "useGetAvailableEvents";
    public static final int REMOTE_CACHE_EXPIRATION = 7200; // 1.5 hr


    // requests and permissions
    public static final int REQUEST_CAMERA = 100;
    public static final int REQUEST_IMAGE = 101;
    public static final int PERMISSION_CAMERA = 102;
    public static final int PERMISSION_GALLERY = 103;

    // other
    public static final String OS_ANDROID = "android";
}

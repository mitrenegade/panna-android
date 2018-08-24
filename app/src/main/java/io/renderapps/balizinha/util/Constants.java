package io.renderapps.balizinha.util;

/**
 * Constants utilized throughout the app(endpoints, permissions, flags etc)
 */

public class Constants {
    public static final String APP_VERSION = "0.3.7";
    public static final String PAYMENT_CONFIG_KEY = "paymentRequired";
    public static final int CACHE_EXPIRATION = 7200; // 1.5 hr
    public static final boolean IN_DEV_MODE = false;

    // firebase endpoints
    public static final String REF_ACTIONS = "actions";
    public static final String REF_PLAYERS = "players";
    public static final String REF_EVENTS = "events";
    public static final String REF_EVENT_USERS = "eventUsers";
    public static final String REF_USER_EVENTS = "userEvents";
    public static final String REF_LEAGUES = "leagues";
    public static final String REF_LEAGUE_PLAYERS = "leaguePlayers";
    public static final String REF_CHARGES = "charges";

    // requests and permissions
    public static final int REQUEST_CAMERA = 100;
    public static final int REQUEST_IMAGE = 101;
    public static final int PERMISSION_CAMERA = 102;
    public static final int PERMISSION_GALLERY = 103;

    // preferences
    public static final String PREF_SHOW_UPDATES_KEY = "pref_updates";
    public static final String PREF_ELAPSED_TIME = "pref_elapse";

    // other
    public static final String OS_ANDROID = "android";
}

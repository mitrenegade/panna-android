package io.renderapps.balizinha.util;

/**
 * Constants utilized throughout the app(endpoints ref, flags etc)
 */

public class Constants {
    public static final String APP_VERSION = "0.3.3";
    public static final String PAYMENT_CONFIG_KEY = "paymentRequired";
    public static final int CACHE_EXPIRATION = 5400; // 1.5 hr
    public static final boolean IN_DEV_MODE = false;

    // firebase endpoints
    public static final String REF_ACTIONS = "actions";
    public static final String REF_PLAYERS = "players";
}

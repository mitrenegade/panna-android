package io.renderapps.balizinha.service;

import java.util.Date;

public class EventService {

    public static boolean isEventOver(long endTimeSec){
        return new Date().getTime() > (endTimeSec * 1000);
    }
}

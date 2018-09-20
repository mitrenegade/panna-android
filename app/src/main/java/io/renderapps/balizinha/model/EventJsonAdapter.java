package io.renderapps.balizinha.model;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.math.BigDecimal;


public class EventJsonAdapter {
    @FromJson
    Event eventFromJson(EventJson eventJson) {
        Event event = new Event();
        event.name = eventJson.name;
        event.photoUrl = eventJson.photoUrl;
        event.city = eventJson.city;
        event.info = eventJson.info;
        event.owner = eventJson.owner;
        event.organizer = eventJson.organizer;
        event.type = eventJson.type;
        event.place = eventJson.place;
        event.state = eventJson.state;
        event.league = eventJson.league;

        event.lat = eventJson.lat;
        event.lon = eventJson.lon;
        event.startTime  = eventJson.startTime;
        event.endTime = eventJson.endTime;
        event.maxPlayers = eventJson.maxPlayers;
        event.amount = eventJson.amount;

        event.active = eventJson.active;
        event.paymentRequired = eventJson.paymentRequired;
        event.leagueIsPrivate = eventJson.leagueIsPrivate;

        BigDecimal bigDecimal = new BigDecimal(eventJson.createdAt);
        event.startTime = bigDecimal.longValue();
        return event;
    }

    @ToJson
    EventJson eventToJson(Event event) {
        EventJson json = new EventJson();
        json.name = event.name;
        //TODO ..
        return json;
    }
}


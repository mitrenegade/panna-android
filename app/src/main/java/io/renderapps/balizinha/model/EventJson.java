package io.renderapps.balizinha.model;

public class EventJson {
    public String eid;
    public String photoUrl;
    public String city;
    public String info;
    public String name;
    public String owner;
    public String organizer;
    public String type;
    public String place;
    public String state;
    public String league;

    public double lat;
    public double lon;
    public long startTime;
    public long endTime;
    public int maxPlayers;

    public double amount;
    public double createdAt;

    public boolean paymentRequired;

    public boolean active = true;
    public boolean leagueIsPrivate;
}

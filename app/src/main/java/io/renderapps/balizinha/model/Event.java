package io.renderapps.balizinha.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Stream;

/**
 * Created by joel on 12/13/17.
 */

public class Event implements Parcelable, ClusterItem {

    public enum Type {
        event3v3 ("3 vs 3"),
        event5v5 ("5 vs 5"),
        event7v7 ("7 vs 7"),
        event11v11 ("11 vs 11"),
        group ("Group class"),
        social ("Social event");

        private final String type;

        Type(String s) {
            type = s;
        }

        public boolean equalsName(String otherName) {
            return type.equals(otherName);
        }

        public String toString() {
            return this.type;
        }

        public static String[] names() {
            return Arrays.toString(Type.values()).replaceAll("^.|.$", "").split(", ");
        }

        public static Type findTypeByName(String name){
            for(Type v : values()){
                if( v.name().equals(name)){
                    return v;
                }
            }
            return null;
        }
    }


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
    public String shareLink;

    public double lat;
    public double lon;
    public long startTime;
    public long endTime;
    public long createdAt;
    public int maxPlayers;
    public double amount;

    public boolean paymentRequired;
    public boolean active;
    public boolean leagueIsPrivate;

    public Event() { }


    /**************************************************************************************************
     * Parcelable
     *************************************************************************************************/

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    private Event(Parcel in){

        this.eid = in.readString();
        this.photoUrl = in.readString();
        this.city = in.readString();
        this.info = in.readString();
        this.name = in.readString();
        this.owner = in.readString();
        this.organizer = in.readString();
        this.type = in.readString();
        this.state = in.readString();
        this.place = in.readString();
        this.league = in.readString();
        this.shareLink = in.readString();

        this.lat = in.readDouble();
        this.lon = in.readDouble();
        this.amount = in.readDouble();

        this.startTime = in.readLong();
        this.endTime = in.readLong();
        this.createdAt = in.readLong();

        this.maxPlayers = in.readInt();

        // boolean read as int, 1 == true, 0 == false
        this.paymentRequired = in.readInt() != 0;
        this.active = in.readInt() != 0;
        this.leagueIsPrivate = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.eid);
        dest.writeString(this.photoUrl);
        dest.writeString(this.city);
        dest.writeString(this.info);
        dest.writeString(this.name);
        dest.writeString(this.owner);
        dest.writeString(this.organizer);
        dest.writeString(this.type);
        dest.writeString(this.state);
        dest.writeString(this.place);
        dest.writeString(this.league);
        dest.writeString(this.shareLink);

        dest.writeDouble(this.lat);
        dest.writeDouble(this.lon);
        dest.writeDouble(this.amount);

        dest.writeLong(this.startTime);
        dest.writeLong(this.endTime);
        dest.writeLong(this.createdAt);

        dest.writeInt(this.maxPlayers);

        // boolean written as int, 1 == true, 0 == false
        dest.writeInt(paymentRequired ? 1 : 0);
        dest.writeInt(active ? 1 : 0);
        dest.writeInt(leagueIsPrivate ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Event){
            if (eid != null && ((Event) obj).eid != null){
                if (eid.equals(((Event) obj).eid))
                    return true;
            }
        }
        return false;
    }

    /**************************************************************************************************
     * Clustering
     *************************************************************************************************/

    @Override
    public LatLng getPosition() {
        return new LatLng(lat, lon);
    }

    @Override
    public String getTitle() {
        return (name != null) ? name : "";
    }

    @Override
    public String getSnippet() {
        return null;
    }

    /**************************************************************************************************
     * Setters & Getters
     *************************************************************************************************/

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getEid() {
        return eid;
    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public String getCity() {
        return city;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getOwner() {
        return owner;
    }

    public String getPlace() {
        return place;
    }

    public String getType() {
        return type;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getState() {
        return state;
    }

    public double getAmount() {
        return amount;
    }

    public boolean getActive() {
        return active;
    }

    public String getOrganizer() {
        return organizer;
    }

    public String getLeague() {
        return league;
    }

    public boolean isLeagueIsPrivate() {
        return leagueIsPrivate;
    }

    public String getShareLink() {
        return shareLink;
    }

    // setters

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setPaymentRequired(boolean paymentRequired) {
        this.paymentRequired = paymentRequired;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }
}

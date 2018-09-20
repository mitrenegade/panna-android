package io.renderapps.balizinha.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

public class League implements Parcelable {

    @NonNull
    private String name;
    private String id;
    private String owner;
    private String city;
    private String info;
    private String photoUrl;

    private int playerCount;
    private int eventCount;

    private boolean isPrivate;
    private long createdAt;

    @Nullable
    private ArrayList<String> tags;

    private League(){ }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof League){
            League league = (League) obj;
            return league.name.equals(name);
        }
        return false;
    }

    /**************************************************************************************************
     * Parcelable
     *************************************************************************************************/

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public League createFromParcel(Parcel in) {
            return new League(in);
        }
        public League[] newArray(int size) {
            return new League[size];
        }
    };

    private League(Parcel in){
        this.name = in.readString();
        this.id = in.readString();
        this.owner = in.readString();
        this.city = in.readString();
        this.info = in.readString();
        this.photoUrl = in.readString();

        this.eventCount = in.readInt();
        this.playerCount = in.readInt();

        this.tags = new ArrayList<>();
        in.readStringList(this.tags);

        this.createdAt = in.readLong();

        // boolean read as int, 1 == true, 0 == false
        this.isPrivate = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.id);
        dest.writeString(this.owner);
        dest.writeString(this.city);
        dest.writeString(this.info);
        dest.writeString(this.photoUrl);

        dest.writeInt(this.playerCount);
        dest.writeInt(this.eventCount);

        dest.writeStringList(this.tags);

        dest.writeLong(this.createdAt);

        // boolean written as int, 1 == true, 0 == false
        dest.writeInt(isPrivate ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    /**************************************************************************************************
     * Setters & Getters
     *************************************************************************************************/
    public void setCity(String city) {
        this.city = city;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }


    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getCity() {
        return city;
    }

    public String getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getId() {
        return id;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public void setIsPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isIsPrivate() {
        return isPrivate;
    }
}

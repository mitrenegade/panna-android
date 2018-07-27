package io.renderapps.balizinha.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.util.Constants;

/**
 * Created by joel on 12/6/17.
 */

public class Player implements Parcelable {

    private String uid;
    private String email;
    private String name;
    private String photoUrl;
    private String city;
    private String info;
    private String deviceToken;
    private String fcmToken;
    private String promotionId;
    private String os;
    private String version;
    private boolean isOwner;
    private long createdAt;

    public Player() {
        // required empty constructor
    }

    public Player(String email) {
        this.name = "";
        this.city = "";
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.version = Constants.APP_VERSION;
        this.os = Constants.OS_ANDROID;
    }

    // getters
    public String getEmail() {
        return email;
    }

    public double getCreatedAt() {
        return createdAt;
    }

    public String getCity() {
        return city;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public String getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public String getUid() {
        return uid;
    }

    public String getOs() {
        return os;
    }

    public String getVersion() {
        return version;
    }

    // setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    // Parcelling part
    public Player(Parcel in){
        String[] data = new String[4];

        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.uid = data[0];
        this.name = data[1];
        this.photoUrl = data[2];
        if (data[3] != null)
            this.city = data[3];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // all we need for now when passing data through bundle
        dest.writeStringArray(new String[] {
                this.uid,
                this.name,
                this.photoUrl,
                this.city
                });
    }
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        public Player[] newArray(int size) {
            return new Player[size];
        }
    };

}

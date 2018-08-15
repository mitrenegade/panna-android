package io.renderapps.balizinha.model;

import android.os.Parcel;
import android.os.Parcelable;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Player){
            Player player = (Player) obj;
            return player.uid != null && player.uid.equals(this.uid);
        }
        return false;
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


    /**************************************************************************************************
     * Parcelable
     *************************************************************************************************/

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        public Player[] newArray(int size) {
            return new Player[size];
        }
    };

    private Player(Parcel in){

        this.uid = in.readString();
        this.name = in.readString();
        this.city = in.readString();
        this.info = in.readString();
        this.email = in.readString();
        this.photoUrl = in.readString();
        this.deviceToken = in.readString();
        this.fcmToken = in.readString();
        this.promotionId = in.readString();
        this.os = in.readString();
        this.version = in.readString();

        // boolean read as int, 1 == true, 0 == false
        this.isOwner = in.readInt() != 0;

        this.createdAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.name);
        dest.writeString(this.city);
        dest.writeString(this.info);
        dest.writeString(this.email);
        dest.writeString(this.photoUrl);
        dest.writeString(this.deviceToken);
        dest.writeString(this.fcmToken);
        dest.writeString(this.promotionId);
        dest.writeString(this.os);
        dest.writeString(this.version);

        dest.writeLong(this.createdAt);

        // boolean written as int, 1 == true, 0 == false
        dest.writeInt(isOwner ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

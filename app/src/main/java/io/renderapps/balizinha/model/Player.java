package io.renderapps.balizinha.model;

import java.util.Calendar;

/**
 * Created by joel on 12/6/17.
 */

public class Player {

    private String pid;
    private String email;
    private String name;
    private String photoUrl;
    private String city;
    private String info;
    private String deviceToken;
    private String fcmToken;
    private String promotionId;
    private boolean isOwner;
    private long createdAt;

    public Player() {
        // required empty constructor
    }

    public Player(String email) {
        this.email = email;
        this.createdAt = System.currentTimeMillis();
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

    public String getPid() {
        return pid;
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

    public void setPid(String pid) {
        this.pid = pid;
    }
}

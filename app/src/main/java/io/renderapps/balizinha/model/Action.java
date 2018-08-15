package io.renderapps.balizinha.model;

/**
 * Created by joel on 12/21/17.
 */

public class Action {
    public static final String ACTION_JOIN = "joinEvent";
    public static final String ACTION_LEAVE = "leaveEvent";
    public static final String ACTION_CREATE = "createEvent";
    public static final String ACTION_CHAT = "chat";
    public static final String ACTION_PAID = "payForEvent";
    public static final String ACTION_HOLD = "holdPaymentForEvent";

    private double createdAt;
    private String event;
    private String type;
    private String message;
    private String user;
    private String username;

    public Action(){
        // required
    }

    public Action(String eid, String message, double time, String type, String uid){
        this.event = eid;
        this.createdAt = time;
        this.type = type;
        this.user = uid;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public double getCreatedAt() {
        return createdAt;
    }

    public String getEvent() {
        return event;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCreatedAt(double createdAt) {
        this.createdAt = createdAt;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}

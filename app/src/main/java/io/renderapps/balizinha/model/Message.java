package io.renderapps.balizinha.model;

/**
 * Created by joel on 12/21/17.
 */

public class Message {

    private String message;
    private String uid;
    private String name;
    private String photoUrl;

    public Message(){}

    public Message(String name, String uid, String message){
        this.name = name;
        this.uid = uid;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public String getMessage() {
        return message;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    // setters

    public void setName(String name) {
        this.name = name;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}

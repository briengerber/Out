package com.bgdev.out.backend;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
@Cache
public class UserRecord {

    @Id
    Long id;

    //Indexed
    @Index private String userName;
    @Index private long lastUpdateDtLong;
    @Index private String userProfId;

    //Unindex
    private List<Long> listOfFriends = new ArrayList<>();
    private List<Long> listOfInvites = new ArrayList<>();
    private List<Long> listOfConversations= new ArrayList<>();
    private String bio;

    public UserRecord() {}

    public void setUserName(String name) {
        userName = name;
    }
    public void setUserProfId(String id) {userProfId = id;}
    public void setLastUpdateDtTm(long lastUpdateDtTm) {
        this.lastUpdateDtLong = lastUpdateDtTm;
    }
    public void addFriendToList(Long id){listOfFriends.add(id);}
    public void removeFriendFromList(Long id){listOfFriends.remove(id);}
    public void setListOfFriends(List<Long> list){listOfFriends=list;}
    public void setListOfInvites(List<Long> list){listOfInvites=list;}
    public void addInviteToList(Long id) {listOfInvites.add(id);}
    public void addConversationToList(Long id){listOfConversations.add(id);}
    public void setBio(String b){bio = b;}


    public String getUserName() {return userName;}
    public String getUserProfId() {return userProfId;}
    public long getLastUpdateDtTm() {return lastUpdateDtLong;}
    public List<Long> getListOfFriends(){return listOfFriends;}
    public Long getId() {return id;}
    public List<Long> getListOfInvites(){return listOfInvites;}
    public List<Long> getListOfConversations(){return listOfConversations;}
    public boolean isAlreadyInviteSent(Long id){return (listOfInvites.size()!=0 && listOfInvites.contains(id));}
    public String getBio(){return bio;}

}
package com.bgdev.out.backend;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
@Cache
public class Conversation {

    @Id
    Long id;

    //Unindex
    private List<Long> userIdsList = new ArrayList<>();
    private List<String> userProfIdsList = new ArrayList<>();
    private Long mostRecentTimeStamp;
    private Long mostRecentUser;
    private String mostRecentText;


    public Conversation() {}

    public List<Long> getUserIdsList(){return userIdsList;}
    public List<String> getUserProfIdsList(){return userProfIdsList;}
    public Long getMostRecentTimeStamp(){return mostRecentTimeStamp;}
    public Long getMostRecentUser(){return mostRecentUser;}
    public String getMostRecentText(){return mostRecentText;}
    public Long getId(){return id;}

    public void setUserIdsList(List<Long> ids){userIdsList=ids;}
    public void setMostRecentTimeStamp(long timeStamp){mostRecentTimeStamp=timeStamp;}
    public void setMostRecentUser(long user){mostRecentUser=user;}
    public void setUserProfIdsList(List<String> profIds){userProfIdsList=profIds;}
    public void setMostRecentText(String text){mostRecentText=text;}

    @Entity
    @Cache
    public static class IndMessage{
        @Id
        Long id;

        @Parent Key<Conversation> parentKey;

        @Index
        private Long timeStamp;

        //Unindex
        private Long sent;
        private String content;

        private String userProfId;

        public IndMessage(){}

        public IndMessage(Long sentFrom, String message){
            sent = sentFrom;
            content = message;
            timeStamp = System.currentTimeMillis();
            userProfId = UserRecordEndpoint.findRecord(sentFrom).getUserProfId();
        }

        public Long getMessageSentFrom(){return sent;}
        public Long getMessageTimeStamp(){return timeStamp;}
        public String getMessageContent(){return content;}
        public String getUserProfId(){return userProfId;}
        @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
        public void setParentKey(Key<Conversation> key){parentKey=key;}
    }
}

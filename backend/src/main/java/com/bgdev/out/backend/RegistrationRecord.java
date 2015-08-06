package com.bgdev.out.backend;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * The Objectify object model for device registrations we are persisting
 */
@Entity
@Cache
public class RegistrationRecord {

    @Id
    Long id;

    //Indexed
    @Index private String regId;
    @Index private Long userUniqId;

    public RegistrationRecord(){}

    public String getRegId() {
        return regId;
    }

    public void setRegId(String regId) {
        this.regId = regId;
    }

    public Long getUserUniqId() {
        return userUniqId;
    }

    public void setUserUniqId(Long userUniqId) {
        this.userUniqId = userUniqId;
    }
}
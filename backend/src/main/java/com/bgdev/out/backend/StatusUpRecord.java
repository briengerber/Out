package com.bgdev.out.backend;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.search.GeoPoint;
import com.google.apphosting.datastore.EntityV4;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

@Entity
@Cache
public class StatusUpRecord {
    @Id
    Long id;

    //Indexed
    @Index private Long userId;
    @Index private long statusUpdateLong;
    @Index private List<Long> listOfPeopleGoing = new ArrayList<>();
    //Unindex
    private int status;
    private boolean bLocked;
    private String statusUserName;
    private String statusUserProfId;
    private List<Integer> listOfPeopleConfirmed = new ArrayList<>();
    //private float statusPlaceLat;
    //private float statusPlaceLong;
    private String statusPlaceName;
    private String statusDescription;

    public StatusUpRecord(){}

    public Long getUserId(){return userId;}
    public long getStatusUpdateLong(){return statusUpdateLong;}
    public int getStatus(){return status;}
    public boolean isLocked(){return bLocked;}
    public String getStatusUserProfId() {return statusUserProfId;}
    public String getStatusUserName() {return statusUserName;}
    public List<Long> getListOfPeopleGoing(){return listOfPeopleGoing;}
    public void clearListofPeopleGoing(){
        if (listOfPeopleGoing!=null) listOfPeopleGoing.clear();
        if (listOfPeopleConfirmed!=null) listOfPeopleConfirmed.clear();
    }
    //public float getStatusPlaceLat(){return statusPlaceLat;}
    //public float getStatusPlaceLong(){return statusPlaceLong;}
    public String getStatusPlaceName(){return statusPlaceName;}
    public String getStatusDescription(){return statusDescription;}

    public void setUserId(Long userId){this.userId=userId;}
    public void setStatus(int st){status=st;}
    public void setStatusUpdateLong(long time){statusUpdateLong=time;}
    public void setLocked(boolean yesOrNo){bLocked=yesOrNo;}
    public void setStatusUserProfId(String statusUserProfId) {this.statusUserProfId = statusUserProfId;}
    public void setStatusUserName(String statusUserName) {this.statusUserName = statusUserName;}
    public void setListOfPeopleGoing(List<Long> list){
        if (this.listOfPeopleGoing!=null && list!=null && !list.isEmpty()) {
            for (int i = 0; i < list.size();i++) {
                if (!listOfPeopleGoing.contains(list.get(i)))
                    this.listOfPeopleGoing.add(list.get(i));
                    this.listOfPeopleConfirmed.add(0);
            }
            //if the updated status doesn't contain the previous ones, then remove them
            for (int j=0; j <listOfPeopleGoing.size(); j++){
                if (!list.contains(listOfPeopleGoing.get(j))) {
                    list.remove(listOfPeopleGoing.get(j));
                    listOfPeopleConfirmed.remove(j);
                }
            }
        }
        else if (list!=null && !list.isEmpty()){
            this.listOfPeopleGoing = new ArrayList<>();
            for (int k=0; k < list.size(); k++) {
                this.listOfPeopleGoing.add(list.get(k));
                this.listOfPeopleConfirmed.add(0);
            }
        }
    }
    //public void setStatusPlaceLat(float lat){statusPlaceLat=lat;}
    //public void setStatusPlaceLong(float mLong){statusPlaceLong=mLong;}
    public void setStatusPlaceName(String name){statusPlaceName=name;  }
    public void setStatusDescription(String desc){statusDescription=desc;}

}

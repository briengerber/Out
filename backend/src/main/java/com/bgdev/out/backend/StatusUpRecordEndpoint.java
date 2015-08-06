package com.bgdev.out.backend;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Query;
import com.google.apphosting.datastore.DatastoreV4;
import com.googlecode.objectify.Key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.inject.Named;
import static com.bgdev.out.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(name = "statusUpRecordApi",version = "v7",resource = "statusUpRecord",namespace = @ApiNamespace(ownerDomain = "backend.out.bgdev.com",ownerName = "backend.out.bgdev.com",packagePath = ""))
public class StatusUpRecordEndpoint {

    private static final Logger log = Logger.getLogger(StatusUpRecordEndpoint.class.getName());

    @ApiMethod(name = "queryFriendStatus",httpMethod = ApiMethod.HttpMethod.GET)
    public List<StatusUpRecord> queryFriendStatus(@Named("uniqId") Long uniqId, @Named("limit") Integer limit){
        UserRecord user = UserRecordEndpoint.findRecord(uniqId);
        List<Long> uniqIdList = user.getListOfFriends();
        uniqIdList.add(user.getId());

        if (limit > 0) return ofy().load().type(StatusUpRecord.class).filter("userId in", uniqIdList).order("-statusUpdateLong").limit(limit).list();
        else return ofy().load().type(StatusUpRecord.class).filter("userId in", uniqIdList).order("-statusUpdateLong").list();
    }

    @ApiMethod(name = "queryFriendStatusAfterTime",httpMethod = ApiMethod.HttpMethod.GET)
    public List<StatusUpRecord> queryFriendStatusAfterTime(@Named("uniqId") Long uniqId, @Named("beginTime") Long timeStamp, @Named("limit") Integer limit){
        UserRecord user = UserRecordEndpoint.findRecord(uniqId);
        List<Long> uniqIdList = user.getListOfFriends();
        uniqIdList.add(user.getId());

        if (limit >0 ) return ofy().load().type(StatusUpRecord.class).filter("userId in", uniqIdList).filter("statusUpdateLong <",timeStamp).order("-statusUpdateLong").limit(limit).list();
        else return ofy().load().type(StatusUpRecord.class).filter("userId in", uniqIdList).filter("statusUpdateLong <",timeStamp).order("-statusUpdateLong").list();
    }

    @ApiMethod(name = "createOrUpdateStatus", path="regular",httpMethod = ApiMethod.HttpMethod.POST)
    public List<StatusUpRecord> createOrUpdateStatus(@Named("desc") String desc,@Named("listGoingWith") List<Long> listOfPeopleGoing,@Named("offset") int offset,@Named("status") int status,@Named("uniqId") Long uniqId)    {
        return generalCreateStatus(listOfPeopleGoing, offset, status, uniqId, null, desc);
    }

    @ApiMethod(name = "createOrUpdateStatusNoFriends",path="noFriends2",httpMethod = ApiMethod.HttpMethod.POST)
    public List<StatusUpRecord> createOrUpdateStatusNoFriends(@Named("desc") String desc,@Named("offset") int offset,@Named("status") int status,@Named("uniqId") Long uniqId)    {
        return generalCreateStatus(null, offset, status, uniqId, null,desc);
    }

    @ApiMethod(name = "createOrUpdateStatusNoFriendsWithPlace",path = "noFriendsWithPlace",httpMethod = ApiMethod.HttpMethod.POST)
    public List<StatusUpRecord> createOrUpdateStatusNoFriendsWithPlace(@Named("desc") String desc,@Named("offset") int offset,@Named("place") String place,@Named("status") int status,@Named("uniqId") Long uniqId)    {
        return generalCreateStatus(null,offset,status,uniqId,place,desc);
    }

    @ApiMethod(name = "createOrUpdateStatusFriendsWithPlace",path = "friendsWithPlace",httpMethod = ApiMethod.HttpMethod.POST)
    public List<StatusUpRecord> createOrUpdateStatusFriendsWithPlace(@Named("desc") String desc,@Named("listGoingWitH") List<Long> listOfPeopleGoing,@Named("offset") int offset,@Named("place") String place,@Named("status") int status,@Named("uniqId") Long uniqId)    {
        return generalCreateStatus(listOfPeopleGoing,offset,status,uniqId,place,desc);
    }

    private List<StatusUpRecord> generalCreateStatus(List<Long> listOfPeopleGoing,int offset,int status,Long uniqId, String statusPlaceName,String statusDescription){
        UserRecord rec = UserRecordEndpoint.findRecord(uniqId);
        List<StatusUpRecord> statusList;
        StatusUpRecord recentStatus = null;
        long currLong,prevLong;
        Calendar prev,curr;

        boolean bListOfPeopleGoing = (listOfPeopleGoing==null);

        int prevHour,currHour,prevDay,currDay;

        if (rec !=null){
            statusList = ofy().load().type(StatusUpRecord.class).filter("userId",rec.getId()).order("-statusUpdateLong").list();
            if (statusList!=null && statusList.size()>0) {
                recentStatus = statusList.get(0);
            }
            if (recentStatus!=null) {

                prev = Calendar.getInstance();
                prev.setTimeInMillis(recentStatus.getStatusUpdateLong()+offset);

                curr = Calendar.getInstance();
                curr.setTimeInMillis(System.currentTimeMillis()+offset);

                prevDay=prev.get(Calendar.DAY_OF_YEAR);
                currDay=curr.get(Calendar.DAY_OF_YEAR);
                prevHour=prev.get(Calendar.HOUR_OF_DAY);
                currHour=curr.get(Calendar.HOUR_OF_DAY);

                currLong = System.currentTimeMillis();
                prevLong = recentStatus.getStatusUpdateLong();

                boolean b = ((currLong-prevLong)< 60000);

                int diff = currDay-prevDay;
                //Check logic for locking records
                if (!recentStatus.isLocked() && !b){
                    if (diff>1 || ((diff >-364) && (diff<0))){
                        lockRecord(recentStatus);
                    }
                    else if (diff==1 || diff==-364) {
                        if (currHour > 5) {
                            lockRecord(recentStatus);
                        }

                    }
                    else if (diff==0){
                        if (currHour>=6 && prevHour<6) {
                            lockRecord(recentStatus);
                        }
                    }
                }

                if (!recentStatus.isLocked()){
                    recentStatus.setStatus(status);
                    recentStatus.setStatusUpdateLong(System.currentTimeMillis());

                    //Update the list if it was not null
                    if (!bListOfPeopleGoing) recentStatus.setListOfPeopleGoing(listOfPeopleGoing);
                    else{
                        //Clear the list of people going if you update it later without anyone
                        if (recentStatus.getListOfPeopleGoing()!=null) recentStatus.clearListofPeopleGoing();
                    }

                    //Status place name is not long
                    if (statusPlaceName!=null) recentStatus.setStatusPlaceName(statusPlaceName);
                    else recentStatus.setStatusPlaceName(null);

                    //Set to null if the default is passed in
                    if (statusDescription.equals("3218908fdsjiojfdsaoij213218908fdsajiojdfjioj321")) recentStatus.setStatusDescription(null);
                    else recentStatus.setStatusDescription(statusDescription);

                    ofy().save().entity(recentStatus).now();
                }
                else{
                    createStatus(status,rec,listOfPeopleGoing,statusPlaceName,statusDescription);
                }
            }
            else{
                createStatus(status,rec,listOfPeopleGoing,statusPlaceName,statusDescription);
            }

            //Send back the update of the list of status
            List<Long> uniqIdList = rec.getListOfFriends();
            if (uniqIdList!=null) {
                uniqIdList.add(rec.getId());
                return ofy().load().type(StatusUpRecord.class).filter("userId in", uniqIdList).order("-statusUpdateLong").limit(100).list();
            }
        }

        //This should never be hit
        return null;
    }

    @ApiMethod(name = "remove",path = "status/{id}",httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(StatusUpRecord.class).id(id).now();
    }

    @ApiMethod(name = "updateToLock",httpMethod = ApiMethod.HttpMethod.PUT)
    public void updateStatusToLock(@Named("id") Long id){
        UserRecord rec = UserRecordEndpoint.findRecord(id);
        List<StatusUpRecord> statusList;
        StatusUpRecord status;
        if (rec !=null){
            statusList = ofy().load().type(StatusUpRecord.class).filter("userId",rec.getId()).list();
            Collections.sort(statusList,new Comparator<StatusUpRecord>() {
                @Override
                public int compare(StatusUpRecord o1, StatusUpRecord o2) {
                    return (int)(o2.getStatusUpdateLong()-o1.getStatusUpdateLong());
                }
            });

            status = statusList.get(0);
            status.setLocked(true);
            ofy().save().entity(status).now();
        }

    }


    //Rewrite with a composite filter
    @ApiMethod(name = "queryMyStatuses",path = "queryMy",httpMethod = ApiMethod.HttpMethod.GET)
    public List<StatusUpRecord> queryMyStatuses(@Named("id") Long id) {
        List<StatusUpRecord> myRecords = ofy().load().type(StatusUpRecord.class).filter("userId",id).order("-statusUpdateLong").list();
        List<StatusUpRecord> otherRecords = ofy().load().type(StatusUpRecord.class).filter("listOfPeopleGoing", id).order("-statusUpdateLong").list();
        myRecords.addAll(otherRecords);

        Collections.sort(myRecords, new Comparator<StatusUpRecord>() {
            @Override
            public int compare(StatusUpRecord o1, StatusUpRecord o2) {
                return (int)(o2.getStatusUpdateLong()-o1.getStatusUpdateLong());
            }
        });

        return myRecords;
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(StatusUpRecord.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find Status with ID: " + id);
        }
    }

    private void createStatus(int status, UserRecord rec,List<Long> listOfPeopleGoing, String statusPlaceName, String statusDescription) {
        StatusUpRecord st = new StatusUpRecord();
        st.setUserId(rec.getId());
        st.setStatus(status);
        st.setStatusUpdateLong(System.currentTimeMillis());
        st.setLocked(false);
        st.setStatusUserName(rec.getUserName());
        st.setStatusUserProfId(rec.getUserProfId());
        if (listOfPeopleGoing!=null && !listOfPeopleGoing.isEmpty()){
            st.setListOfPeopleGoing(listOfPeopleGoing);
        }
        if (statusPlaceName!=null) st.setStatusPlaceName(statusPlaceName);

        if (!statusDescription.equals("3218908fdsjiojfdsaoij213218908fdsajiojdfjioj321")) st.setStatusDescription(statusDescription);

        ofy().save().entity(st).now();
    }

    public void lockRecord(StatusUpRecord record){
        record.setLocked(true);
        ofy().save().entity(record).now();
    }
}
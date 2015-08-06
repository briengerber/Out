package com.bgdev.out.backend;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.googlecode.objectify.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import static com.bgdev.out.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(name = "userRecordApi",version = "v3",resource = "userRecord",namespace = @ApiNamespace(ownerDomain = "backend.out.bgdev.com",ownerName = "backend.out.bgdev.com",packagePath = ""))
public class UserRecordEndpoint {

    static final String API_KEY = System.getProperty("gcm.api.key");

    @ApiMethod(name = "createUser",httpMethod = ApiMethod.HttpMethod.POST)
    public UserRecord createUser(@Named("name") String name,@Named("profId") String profId) {
        UserRecord record = ofy().load().type(UserRecord.class).filter("userProfId",profId).first().now();
        if (record!=null) return record;
        else {
            record = new UserRecord();
            record.setUserName(name);
            record.setUserProfId(profId);
            List<Long> list = new ArrayList<>();
            record.setListOfFriends(list);
            ofy().save().entity(record).now();
            return record;
        }
    }

    @ApiMethod(name= "updateUser",httpMethod = ApiMethod.HttpMethod.PUT)
    public UserRecord updateUser(@Named("id") Long id, @Named("name") String name, @Named("userProfId") String profId){
        boolean change = false;
        UserRecord rec = findRecord(id);
        if (rec !=null) {
            if (!name.equals("") && !name.equals(rec.getUserName())) {
                rec.setUserName(name);
                change = true;
            }
            if (!profId.equals("") && !profId.equals(rec.getUserProfId())) {
                rec.setUserProfId(profId);
                change = true;
            }
            if (change) ofy().save().entity(rec).now();
        }
        return rec;
    }

    @ApiMethod(name = "queryAllUsers",httpMethod = ApiMethod.HttpMethod.GET)
    public List<UserRecord> queryAllUsers(){
        return ofy().load().type(UserRecord.class).order("userName").list();
    }

    @ApiMethod(name = "queryFriendsUserRecs",httpMethod = ApiMethod.HttpMethod.GET)
    public List<UserRecord> queryFriendsUserRecs(@Named("uniqId") Long id) {
        List<UserRecord> listUserRecFriends = new ArrayList<>();
        UserRecord myUser = findRecord(id);
        List<Long> friends = myUser.getListOfFriends();
        for (int i = 0; i < friends.size();i++){
            listUserRecFriends.add(findRecord(friends.get(i)));
        }
        return listUserRecFriends;
    }

    @ApiMethod(name = "queryUser",path = "queryUser",httpMethod = ApiMethod.HttpMethod.GET)
    public UserRecord queryUserFromId(@Named("userId") Long id){
        return findRecord(id);
    }

    /*
     * Friend APIs
     */
    @ApiMethod(name = "becomeFriends",path = "friends", httpMethod = ApiMethod.HttpMethod.POST)
    public void becomeFriends(@Named("myUserId") Long myUser,@Named("otherUserId") Long otherUser){
        UserRecord myUserRec = UserRecordEndpoint.findRecord(myUser);
        UserRecord otherUserRec = UserRecordEndpoint.findRecord(otherUser);

        if (!areWeFriends(myUserRec,otherUserRec)) {
            myUserRec.addFriendToList(otherUserRec.getId());
            otherUserRec.addFriendToList(myUserRec.getId());

            if (myUserRec.isAlreadyInviteSent(otherUserRec.getId())) myUserRec.getListOfInvites().remove(otherUserRec.getId());
            if (otherUserRec.isAlreadyInviteSent(myUserRec.getId())) otherUserRec.getListOfInvites().remove(myUserRec.getId());
            ofy().save().entities(myUserRec, otherUserRec).now();
        }
    }

    @ApiMethod(name = "removeFriend",httpMethod = ApiMethod.HttpMethod.PUT)
    public void removeFriend(@Named("myUserId") Long mUser, @Named("otherUser") Long oUserId) {
        UserRecord myUserRec = findRecord(mUser);
        UserRecord otherUserRec = findRecord(oUserId);

        if (areWeFriends(myUserRec,otherUserRec)){
            myUserRec.removeFriendFromList(otherUserRec.getId());
            otherUserRec.removeFriendFromList(myUserRec.getId());
            ofy().save().entities(myUserRec,otherUserRec).now();
        }
    }

    @ApiMethod(name= "rejectFriendRequest",path="rejectRequest",httpMethod = ApiMethod.HttpMethod.PUT)
    public List<UserRecord> rejectFriendRequest(@Named("fromId") Long fromId, @Named("myId") Long myId){
        List<UserRecord> remainingInvites = new ArrayList<>();
        UserRecord me = findRecord(myId);
        List<Long> myInvites = me.getListOfInvites();
        if (me.isAlreadyInviteSent(fromId)) myInvites.remove(fromId);
        ofy().save().entity(me).now();

        for (int i = 0; i < myInvites.size(); i++){
            remainingInvites.add(findRecord(myInvites.get(i)));
        }
        return remainingInvites;
    }

    @ApiMethod(name="queryUserInvites",path="queryInvites",httpMethod = ApiMethod.HttpMethod.GET)
    public List<UserRecord> queryUserInvites(@Named("userId") Long id){
        UserRecord user = findRecord(id);
        List<Long> myInvites = user.getListOfInvites();
        List<UserRecord> userInvites = new ArrayList<>();

        for (int i = 0; i < myInvites.size(); i++){
            userInvites.add(findRecord(myInvites.get(i)));
        }

        return userInvites;
    }

    @ApiMethod(name="clearUserFriend",httpMethod = ApiMethod.HttpMethod.PUT)
    public void clearFriendList(@Named("userId") Long id){
        UserRecord user = findRecord(id);
        user.getListOfFriends().clear();
        ofy().save().entities(user).now();
    }

    @ApiMethod(name="clearUserInviteList",path="clearInviteList", httpMethod = ApiMethod.HttpMethod.PUT)
    public void clearInviteList(@Named("userId") Long id){
        UserRecord user = findRecord(id);
        user.getListOfInvites().clear();
        ofy().save().entities(user).now();
    }

    @ApiMethod(name="clearUserConvList",path="clearConvList", httpMethod = ApiMethod.HttpMethod.PUT)
    public void clearConvList(@Named("userId") Long id){
        UserRecord user = findRecord(id);
        user.getListOfConversations().clear();
        ofy().save().entities(user).now();
    }


    @ApiMethod(name = "sendFriendRequest",path ="friendRequest")
    public void sendFriendRequest(@Named("from") Long fromId, @Named("to") Long toId){
        UserRecord from = UserRecordEndpoint.findRecord(fromId);
        UserRecord to = UserRecordEndpoint.findRecord(toId);

        if (areWeFriends(from,to)) return;
        if (to.isAlreadyInviteSent(from.getId())) return;

        List<String> toRegRecords = RegistrationEndpoint.findRecordsBasedOnId(toId);

        Sender sender = new Sender(API_KEY);

        Message.Builder builder = new Message.Builder();
        builder.addData("fromId",from.getId().toString());
        builder.addData("fromName",from.getUserName());
        builder.addData("type", "friendRequest");
        Message msg = builder.build();

        try{
            MulticastResult results= sender.send(msg,toRegRecords,10);
            if (results.getSuccess()>0){
                to.addInviteToList(from.getId());
                ofy().save().entity(to).now();
            }
            else{
                //log.info("failure");
            }
        }
        catch (IOException e ){
            e.printStackTrace();
            //log.info("exception");
        }
    }


    public static UserRecord findRecord(Long id) {
        return ofy().load().type(UserRecord.class).id(id).now();
    }

    public static List<UserRecord> findRecords(List<Long> ids){
        List<UserRecord> list = new ArrayList<>();
        for (int i = 0;i <ids.size();i++){
            list.add(findRecord(ids.get(i)));
        }
        return list;
    }

    public static boolean areWeFriends(UserRecord myUser, UserRecord otherUserRecord){
        List<Long> myFriends = myUser.getListOfFriends();
        List<Long> otherFriends = otherUserRecord.getListOfFriends();

        if ((myFriends.contains(otherUserRecord.getId())) && otherFriends.contains(myUser.getId())) return true;
        else return false;
    }

}
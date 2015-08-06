package com.bgdev.out.backend;

import com.google.android.gcm.server.*;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.googlecode.objectify.Key;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
@Api(name = "conversationApi",version = "v3",resource = "conversation",namespace = @ApiNamespace(ownerDomain = "backend.out.bgdev.com", ownerName = "backend.out.bgdev.com",packagePath = ""))
public class ConversationEndpoint {

    private static final Logger log = Logger.getLogger(ConversationEndpoint.class.getName());

    static final String API_KEY = System.getProperty("gcm.api.key");

    @ApiMethod(name = "addToConv",httpMethod = ApiMethod.HttpMethod.POST)
    public void addToConv(@Named("fromId") Long fromId, @Named("toId") Long toId, @Named("strMsg") String message){
        List<Long> allIds = new ArrayList<>();
        allIds.add(fromId);
        allIds.add(toId);

        UserRecord fromUser = UserRecordEndpoint.findRecord(fromId);
        String fromName = fromUser.getUserName();

        //Find the current Conversation based on the people in it
        List<Long> listOfConvLongs = fromUser.getListOfConversations();
        Map<Long,Conversation> mapOfConvs = ofy().load().type(Conversation.class).ids(listOfConvLongs);

        Conversation conv=null;
        int totalPeeps = 0;
        for (Conversation c : mapOfConvs.values()){
            for (int i = 0; i <c.getUserIdsList().size();i++) {
                if (allIds.contains(c.getUserIdsList().get(i))) {
                    totalPeeps = totalPeeps+1;
                }
                else break;
            }
            if (totalPeeps==allIds.size()) {
                conv=c;
                break;
            }
        }

        //If the conversation wasn't found create a new one.
        if (conv==null){
            conv = new Conversation();
            conv.setUserIdsList(allIds);
            List<String> profIds = new ArrayList<>();
            for (int i = 0; i < allIds.size(); i++){
                profIds.add(UserRecordEndpoint.findRecord(allIds.get(i)).getUserProfId());
            }
            conv.setUserProfIdsList(profIds);
        }

        conv.setMostRecentTimeStamp(System.currentTimeMillis());
        conv.setMostRecentUser(fromId);
        conv.setMostRecentText(message);

        ofy().save().entities(conv).now();

        Key<Conversation> par = Key.create(Conversation.class,conv.getId());
        Conversation.IndMessage indMessage = new Conversation.IndMessage(fromId,message);
        indMessage.setParentKey(par);
        ofy().save().entities(indMessage).now();

        List <UserRecord> list = UserRecordEndpoint.findRecords(allIds);

        for (int i = 0; i <list.size();i++){
            UserRecord user = list.get(i);
            List<Long> listConv = user.getListOfConversations();
            if (!listConv.contains(conv.getId())) user.addConversationToList(conv.getId());
        }

        ofy().save().entities(list).now();

        //Send the message over GCM
        Sender sender = new Sender(API_KEY);
        Message.Builder builder = new Message.Builder();
        builder.addData("message", message);
        builder.addData("fromID",fromId.toString());
        builder.addData("fromName",fromName);
        builder.addData("type", "message");
        builder.addData("convId",conv.getId().toString());
        Message msg = builder.build();

        try {
            RegistrationRecord record = ofy().load().type(RegistrationRecord.class).filter("userUniqId",toId).first().now();
            Result result = sender.send(msg, record.getRegId(), 1000);
            if (result.getMessageId() != null) {
                log.info("IndMessage sent to " + record.getRegId());
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
                    record.setRegId(canonicalRegId);
                    ofy().save().entity(record).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
        catch (IOException i){
            i.printStackTrace();
        }
    }

    @ApiMethod(name = "sendMessageToConv",path="toConv",httpMethod = ApiMethod.HttpMethod.POST)
    public List<Conversation.IndMessage> sendMessageToConv(@Named("Id") Long fromId, @Named("convId") Long convId,@Named("message") String message) {
        Conversation conversation = findConversation(convId);
        UserRecord from = UserRecordEndpoint.findRecord(fromId);
        List<Long> listIDs = conversation.getUserIdsList();
        List<String> listRegs = new ArrayList<>();

        for (int i=0; i <listIDs.size(); i++){
            List<String> regListString= new ArrayList<>();
            Long l = listIDs.get(i);
            if (!l.equals(fromId)) regListString = RegistrationEndpoint.findRecordsBasedOnId(l);
            for (int j=0; j < regListString.size();j++){
                listRegs.add(regListString.get(j));
            }
        }

        conversation.setMostRecentTimeStamp(System.currentTimeMillis());
        conversation.setMostRecentUser(fromId);
        conversation.setMostRecentText(message);

        Key<Conversation> par = Key.create(Conversation.class,conversation.getId());
        Conversation.IndMessage indMessage = new Conversation.IndMessage(fromId,message);
        indMessage.setParentKey(par);

        ofy().save().entities(indMessage,conversation).now();

        String fromName = from.getUserName();
        Sender sender = new Sender(API_KEY);

        Message.Builder builder = new Message.Builder();
        builder.addData("message", message);
        builder.addData("fromID",fromId.toString());
        builder.addData("fromName", fromName);
        builder.addData("type", "message");
        builder.addData("profId",from.getUserProfId());
        builder.addData("convId",convId.toString());
        Message msg = builder.build();

        try{
            MulticastResult multicastResult = sender.send(msg,listRegs,1000);
            if (multicastResult.getFailure()!=0) {
                log.warning("Error sending message");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return ofy().load().type(Conversation.IndMessage.class).ancestor(conversation).order("timeStamp").list();
    }

    @ApiMethod(name = "queryUsersConversations",httpMethod = ApiMethod.HttpMethod.GET)
    public List<Conversation> queryUsersConversations(@Named("Id") Long id){
        UserRecord userRecord = UserRecordEndpoint.findRecord(id);
        List<Conversation> userConvs = new ArrayList<>();

        for (int i = 0;i <userRecord.getListOfConversations().size(); i++){
            userConvs.add(findConversation(userRecord.getListOfConversations().get(i)));
        }

        Collections.sort(userConvs, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation o1, Conversation o2) {
                return (int)(o1.getMostRecentTimeStamp()-o2.getMostRecentTimeStamp());
            }
        });
        return userConvs;
    }

    @ApiMethod(name = "queryConvMessages",path="ind",httpMethod = ApiMethod.HttpMethod.GET)
    public List<Conversation.IndMessage> queryConvMessages(@Named("ConvID") Long id){
        Conversation conversation = findConversation(id);
        return ofy().load().type(Conversation.IndMessage.class).ancestor(conversation).order("timeStamp").list();
    }

    @ApiMethod(name = "queryUsersConversationsMap",path="map",httpMethod = ApiMethod.HttpMethod.GET)
    public AllConversationList queryUsersConversationsMap(@Named("Id") Long id){
        UserRecord userRecord = UserRecordEndpoint.findRecord(id);
        List<Conversation> userConvs = new ArrayList<>();
        AllConversationList convlist = new AllConversationList();
        for (int i = 0; i <userRecord.getListOfConversations().size();i++){
            userConvs.add(findConversation(userRecord.getListOfConversations().get(i)));
        }

        Collections.sort(userConvs, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation o1, Conversation o2) {
                return (int) (o1.getMostRecentTimeStamp() - o2.getMostRecentTimeStamp());
            }
        });

        for (int i = 0; i < userConvs.size(); i ++){
            List<Conversation.IndMessage> temp = ofy().load().type(Conversation.IndMessage.class).ancestor(userConvs.get(i)).order("timeStamp").list();
            convlist.addToMessageList(userConvs.get(i).getId(),temp);
        }

        return convlist;
    }

    @ApiMethod(name = "sendTypingUpdate",path="type",httpMethod = ApiMethod.HttpMethod.PUT)
    public void sendTypingUpdate(@Named("Id") Long id,@Named("state") String state, Conversation conv){

        Message.Builder builder = new Message.Builder();
        builder.addData("sendUserId",id.toString());
        builder.addData("type", "typing");
        builder.addData("convId", conv.getId().toString());
        builder.addData("state",state);

        Message msg = builder.build();

        List<Long> sendIds = conv.getUserIdsList();
        List<String> regIds = new ArrayList<>();
        for (int i = 0; i < sendIds.size(); i++){
            if (!sendIds.get(i).equals(id)){
                RegistrationEndpoint.addRegIdsToList(sendIds.get(i),regIds);
            }
        }

        Sender sender = new Sender(API_KEY);

        try{
            MulticastResult multicastResult = sender.send(msg,regIds,1000);
            if (multicastResult.getFailure()!=0) {
                log.warning("Error sending message");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public static Conversation findConversation(Long id){
        return ofy().load().type(Conversation.class).id(id).now();
    }

    public static Conversation.IndMessage findIndMessages(Long id){
        return ofy().load().type(Conversation.IndMessage.class).id(id).now();
    }


}
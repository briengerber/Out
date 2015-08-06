package com.bgdev.out;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bgdev.out.backend.conversationApi.ConversationApi;
import com.bgdev.out.backend.conversationApi.model.AllConversationList;
import com.bgdev.out.backend.conversationApi.model.AllConversationMap;
import com.bgdev.out.backend.conversationApi.model.Conversation;
import com.bgdev.out.backend.conversationApi.model.IndConvListObject;
import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.bgdev.out.backend.conversationApi.model.JsonMap;
import com.bgdev.out.backend.registration.Registration;
import com.bgdev.out.backend.registration.model.RegistrationRecord;
import com.bgdev.out.backend.statusUpRecordApi.StatusUpRecordApi;
import com.bgdev.out.backend.statusUpRecordApi.model.StatusUpRecord;
import com.bgdev.out.backend.userRecordApi.UserRecordApi;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.google.android.gms.games.Notifications;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class MyAsyncTasks {

    public static Registration regService;
    public static GoogleCloudMessaging gcm;
    public static StatusUpRecordApi statusApi;
    public static UserRecordApi userRecordApi;
    public static ConversationApi conversationApi;

    public static void registerRegService(){
        Registration.Builder builder = new Registration.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
        builder.setRootUrl("https://" + Globals.APP_ENGINE_APP_ID + ".appspot.com/_ah/api/");
        regService = builder.build();
    }

    public static void registerStatusService(){
        StatusUpRecordApi.Builder builder= new StatusUpRecordApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
        builder.setRootUrl("https://" + Globals.APP_ENGINE_APP_ID + ".appspot.com/_ah/api/");
        statusApi = builder.build();
    }

    public static void registerUserRecord(){
        UserRecordApi.Builder builder= new UserRecordApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
        builder.setRootUrl("https://" + Globals.APP_ENGINE_APP_ID + ".appspot.com/_ah/api/");
        userRecordApi = builder.build();
    }

    public static void registerConversationService(){
        ConversationApi.Builder builder= new ConversationApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
        builder.setRootUrl("https://" + Globals.APP_ENGINE_APP_ID + ".appspot.com/_ah/api/");
        conversationApi = builder.build();
    }
    public static class GcmRegistrationAsyncTask extends AsyncTask<Context, Void, String> {
        Context context;
        RegistrationRecord record;

        public GcmRegistrationAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Context... params) {
            if (regService == null) registerRegService();

            String msg = "";
            try {
                if (gcm == null) gcm = GoogleCloudMessaging.getInstance(context);

                if (Globals.DEVICE_REG_ID.equals("")) {
                    Globals.DEVICE_REG_ID = gcm.register(Globals.GCM_SENDER_ID);

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    record = regService.register(Globals.DEVICE_REG_ID, Globals.UNIQ_ID).execute();

                    Globals.editor = Globals.settings.edit();
                    Globals.editor.putString(Globals.PROPERTY_REG_ID, record.getRegId());
                    Globals.editor.commit();
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                msg = "Error in GCM: " + ex.getMessage();
            }

            Log.e("Tag", msg);
            return msg;
        }
    }

    public static class CreateUserAsyncTask extends AsyncTask<Void,Void,Void>{
        String name,userProfId;
        Context context;

        public CreateUserAsyncTask(Context c, String profId, String userName){
            name = userName;
            userProfId=profId;
            context = c;
        }
        @Override
        protected Void doInBackground(Void... params) {

            if (userRecordApi==null) registerUserRecord();

            Globals.editor = Globals.settings.edit();
            try{
                if (Globals.UNIQ_ID==0)  {
                    Globals.myUser = userRecordApi.createUser(name,userProfId).execute();
                    Globals.UNIQ_ID = Globals.myUser.getId();
                    Globals.USER_NAME = Globals.myUser.getUserName();
                    Globals.USER_PROF_ID = Globals.myUser.getUserProfId();

                    Globals.editor.putLong(Globals.PROPERTY_UNIQ_ID, Globals.UNIQ_ID);
                    Globals.editor.putString(Globals.USER_NAME_KEY, Globals.USER_NAME);
                    Globals.editor.putString(Globals.USER_PROF_ID_KEY,Globals.USER_PROF_ID);
                    Globals.editor.commit();
                }
                else {
                    if (Globals.myUser==null) Globals.myUser = userRecordApi.queryUser(Globals.UNIQ_ID).execute();

                    if ((!userProfId.equals(Globals.myUser.getUserProfId())) || (!name.equals(Globals.myUser.getUserName()))){
                        Globals.myUser = userRecordApi.updateUser(Globals.myUser.getId(),name,userProfId).execute();
                    }

                    Globals.USER_NAME = Globals.myUser.getUserName();
                    Globals.USER_PROF_ID = Globals.myUser.getUserProfId();

                    Globals.editor.putString(Globals.USER_NAME_KEY,Globals.USER_NAME);
                    Globals.editor.putString(Globals.USER_PROF_ID_KEY,Globals.USER_PROF_ID );
                    Globals.editor.commit();
                }
            }

            catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void b) {
            new GcmRegistrationAsyncTask(context).execute();
            if (Globals.userFriendList==null) new GetUserFriendsAsyncTask().execute();
            if (Globals.statusUpRecordList==null) new QueryStatusUpdateAsyncTask(true).execute();
            if (Globals.friendRequestLists==null) new QueryFriendInvitesAsyncTask().execute();
            if (Globals.allUserList==null) new QueryAllUsersAsyncTask().execute();
            if (Globals.listOfConversations==null) new QueryActiveConversationsAsyncTask().execute();
        }
    }

    public static class GetUserFriendsAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (userRecordApi==null) registerUserRecord();
            try{
                if (Globals.UNIQ_ID!=null && Globals.UNIQ_ID!=0) Globals.userFriendList = userRecordApi.queryFriendsUserRecs(Globals.UNIQ_ID).execute().getItems();
            }

            catch (IOException ex){
                ex.printStackTrace();
            }

            return null;
        }
    }

    public static class QueryAllUsersAsyncTask extends AsyncTask<Void,Void,Void>{

        public QueryAllUsersAsyncTask() {}

        @Override
        protected Void doInBackground(Void... params) {
            if (userRecordApi == null) registerUserRecord();

            try{
                Globals.allUserList=userRecordApi.queryAllUsers().execute().getItems();
            }

            catch (IOException ex){
                ex.printStackTrace();
            }
            return null;
        }
    }

    public static class QueryStatusUpdateAsyncTask extends AsyncTask<Void,Void,Void>{
        boolean bRefreshed = false,bFirstCreate=false;
        AsyncDelegate mAsyncDelegate;

        int limitOtherThanDefault = 1;
        public QueryStatusUpdateAsyncTask(boolean firstCreate){
            bFirstCreate=firstCreate;
        }

        public QueryStatusUpdateAsyncTask(boolean firstCreate, int limit){
            bFirstCreate=firstCreate;
            limitOtherThanDefault=limit;
        }

        public QueryStatusUpdateAsyncTask(boolean ref,AsyncDelegate asyncDelegate){
            bRefreshed=ref;
            mAsyncDelegate = asyncDelegate;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (statusApi==null) registerStatusService();

            try{
                if (Globals.UNIQ_ID!=0) {
                    if (limitOtherThanDefault==1) Globals.statusUpRecordList = statusApi.queryFriendStatus(Globals.UNIQ_ID,Globals.numberOfStatusToLoad).execute().getItems();
                    else Globals.statusUpRecordList = statusApi.queryFriendStatus(Globals.UNIQ_ID,limitOtherThanDefault).execute().getItems();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (bFirstCreate){
                if (HomeFragment.mHomeRecyclerView!=null) {
                    HomeFragment.statusUpRecordList = Globals.statusUpRecordList;
                    HomeFragment.mHomeAdapter = new HomeFragment.HomeRecyclerViewAdapter(HomeFragment.statusUpRecordList);
                    HomeFragment.mHomeRecyclerView.setAdapter(HomeFragment.mHomeAdapter);
                }
            }
            else if (bRefreshed) mAsyncDelegate.asyncComplete(true);

            if (HomeFragment.progressBar!=null && HomeFragment.progressBar.getVisibility()==View.VISIBLE) HomeFragment.progressBar.setVisibility(View.GONE);
        }
    }

    public static class QueryStatusUpdateFromScroll extends AsyncTask<Void,Void,Void>{
        UpdateStatusInterface updateStatusInterface;
        int position;
        List<StatusUpRecord> list = new ArrayList<>();
        public QueryStatusUpdateFromScroll(int listPosition,UpdateStatusInterface sync){
            updateStatusInterface = sync;
            position=listPosition;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (statusApi==null) registerStatusService();
            try{
                if (Globals.UNIQ_ID!=0) {
                    //-1 because of the headerview
                    Long timeStamp = Globals.statusUpRecordList.get(position-1).getStatusUpdateLong();
                    list = statusApi.queryFriendStatusAfterTime(Globals.UNIQ_ID,timeStamp,Globals.numberOfStatusToLoad).execute().getItems();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateStatusInterface.updateFromScroll(list);
        }
    }

    public static class CreateOrUpdateStatusAsyncTask extends AsyncTask<Void,Void,Void>{
        int status;
        Context context;
        List<Long> friendsGoingWith= new ArrayList<>();
        String placeString;
        String description="3218908fdsjiojfdsaoij213218908fdsajiojdfjioj321";
        UpdateStatusInterface updateStatusInterface;
        List<StatusUpRecord> returnStatusList = new ArrayList<>();
        public CreateOrUpdateStatusAsyncTask(Context ct, int stat){
            context=ct;
            status=stat;
        }


        public CreateOrUpdateStatusAsyncTask(Context ct, int stat,List<Long>list,String place,String desc){
            context=ct;
            status=stat;
            friendsGoingWith=list;
            placeString=place;
            description = desc;
        }


        public CreateOrUpdateStatusAsyncTask(Context ct, int stat,List<Long>list,String place,UpdateStatusInterface intf){
            context=ct;
            status=stat;
            friendsGoingWith=list;
            placeString=place;
            updateStatusInterface = intf;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (statusApi==null) registerStatusService();

            try{
                if (Globals.UNIQ_ID!=null && Globals.UNIQ_ID!=0) {
                    int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
                    if (description==null || description.equals("")) description="3218908fdsjiojfdsaoij213218908fdsajiojdfjioj321";

                    //No people were attached to the status
                    if (friendsGoingWith==null || friendsGoingWith.isEmpty()) {
                        //No place was attached to the status
                        if (placeString==null) {
                            returnStatusList = statusApi.createOrUpdateStatusNoFriends(description,offset, status, Globals.UNIQ_ID).execute().getItems();
                        }
                        //A place was attached but no people
                        else{
                            returnStatusList = statusApi.createOrUpdateStatusNoFriendsWithPlace(description,offset,placeString, status, Globals.UNIQ_ID).execute().getItems();
                        }
                    }
                    //A list of people were attached to the status
                    else {
                        //No place was attached to the status
                        if (placeString==null) {
                            returnStatusList = statusApi.createOrUpdateStatus(description,friendsGoingWith,offset,status,Globals.UNIQ_ID).execute().getItems();
                        }
                        //A place was attached to the status, yes people
                        else{
                            returnStatusList = statusApi.createOrUpdateStatusFriendsWithPlace(description,friendsGoingWith, offset,placeString, status, Globals.UNIQ_ID).execute().getItems();
                        }
                    }
                }
                Globals.statusUpRecordList = returnStatusList;
            }
            catch (IOException e){e.printStackTrace();}

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (updateStatusInterface!=null && returnStatusList!=null) updateStatusInterface.update(returnStatusList);
            else if (returnStatusList!=null){
                HomeFragment.statusUpRecordList = returnStatusList;
                HomeFragment.mHomeRecyclerView.setAdapter(new HomeFragment.HomeRecyclerViewAdapter(returnStatusList));
            }
        }
    }

    public static class RejectFriendRequestAsyncTask extends AsyncTask<Void,Void,Void> {
        Long fromId,myId;
        public RejectFriendRequestAsyncTask(Long fId, Long mId){
            fromId=fId;
            myId=mId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (userRecordApi==null) registerUserRecord();

            try{
                Globals.friendRequestLists = userRecordApi.rejectFriendRequest(fromId,myId).execute().getItems();
                if (Globals.friendRequestLists==null) Globals.friendRequestLists= new ArrayList<>();
            }
            catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }
    }

    public static class QueryFriendInvitesAsyncTask extends AsyncTask<Void,Void,Boolean>{

        boolean bRef=false;

        public QueryFriendInvitesAsyncTask(){}
        public QueryFriendInvitesAsyncTask(boolean b){
            bRef=b;
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            if (userRecordApi==null) registerUserRecord();

            try{
                Globals.friendRequestLists = userRecordApi.queryUserInvites(Globals.UNIQ_ID).execute().getItems();
                if (Globals.friendRequestLists==null) {
                    Globals.friendRequestLists= new ArrayList<>();
                    return Boolean.FALSE;
                }
                else {
                    return Boolean.TRUE;
                }

            }
            catch (IOException e){
                e.printStackTrace();
            }

            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if (b && InvitesFragment.mInvitesRecyclerView!=null) InvitesFragment.mInvitesRecyclerView.setAdapter(new InvitesFragment.InvitesRecyclerViewAdapter(Globals.friendRequestLists));
            if (bRef) InvitesFragment.swipeRefreshLayout.setRefreshing(false);
        }
    }

    public static class QueryActiveConversationsAsyncTask extends AsyncTask<Void,Void,Void>{
        boolean bRefreshed = false;
        AsyncDelegate asyncDelegate;

        public QueryActiveConversationsAsyncTask(){}

        public QueryActiveConversationsAsyncTask(boolean ref,AsyncDelegate asyncDelegate){
            this.asyncDelegate=asyncDelegate;
            bRefreshed=ref;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (conversationApi==null) registerConversationService();

            try{
                Globals.listOfConversations = conversationApi.queryUsersConversations(Globals.UNIQ_ID).execute().getItems();
                AllConversationList temp = conversationApi.queryUsersConversationsMap(Globals.UNIQ_ID).execute();
                for (int i = 0; i <temp.getMessageList().size();i++){
                    IndConvListObject obj = temp.getMessageList().get(i);
                    Globals.allConvIndMessMap.put(obj.getConversationid(),obj.getListOfIndMessages());
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (bRefreshed) asyncDelegate.asyncComplete(true);
        }
    }

    public static class SendIndMessageAsyncTask extends AsyncTask<Void,Void,Void>{

        Conversation conversation;
        String message;
        public SendIndMessageAsyncTask(Conversation conv,String msg){
            conversation=conv;
            message = msg;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (conversationApi==null) registerConversationService();

            try {
                List<IndMessage> indLlist = conversationApi.sendMessageToConv(Globals.UNIQ_ID, conversation.getId(), message).execute().getItems();
                Globals.listOfConversations = conversationApi.queryUsersConversations(Globals.UNIQ_ID).execute().getItems();
                AllConversationList temp = conversationApi.queryUsersConversationsMap(Globals.UNIQ_ID).execute();
                for (int i = 0; i <temp.getMessageList().size();i++){
                    IndConvListObject obj = temp.getMessageList().get(i);
                    Globals.allConvIndMessMap.put(obj.getConversationid(),obj.getListOfIndMessages());
                }
            }
            catch (IOException i){
                i.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (MessagesFragment.mMessagesAdapter!=null) MessagesFragment.mMessagesAdapter.notifyDataSetChanged();
        }
    }

    public static class QueryUserStatusForOneUser extends AsyncTask<Void,Void,Void>{
        Long mId;
        List<StatusUpRecord> returnList;
        UpdateStatusInterface updateStatusInterface;
        List<StatusUpRecord> mList;

        public QueryUserStatusForOneUser(Long id,UpdateStatusInterface uInterface,List<StatusUpRecord> list){
            mId=id;
            updateStatusInterface=uInterface;
            mList = list;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (statusApi==null) registerStatusService();

            try{
                returnList = statusApi.queryMyStatuses(mId).execute().getItems();
            }
            catch (IOException i){
                i.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            updateStatusInterface.update(returnList);
        }
    }

    public static class SendTypingUpdateAsyncTask extends AsyncTask<Void,Void,Void>{
        Conversation conversation;
        String state;
        public SendTypingUpdateAsyncTask(Conversation conv,String st){
            conversation = conv;
            state = st;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (conversationApi==null) registerConversationService();

            try{
                conversationApi.sendTypingUpdate(Globals.UNIQ_ID,state,conversation).execute();
            }
            catch (IOException i){i.printStackTrace();}
            return null;
        }
    }

    public static class UpdateProfPicBitMap extends AsyncTask<Void,Void,Void>{
        ImageView circleImageView;

        public UpdateProfPicBitMap(ImageView circle){
            circleImageView = circle;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Globals.profPicBitMap=StaticConvertMethods.returnBigPicBitMap(Globals.USER_PROF_ID,1);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (Globals.profPicBitMap!=null) {
                circleImageView.setImageBitmap(Globals.profPicBitMap);
            }
        }
    }

}

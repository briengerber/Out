package com.bgdev.out;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.bgdev.out.backend.conversationApi.model.AllConversationList;
import com.bgdev.out.backend.conversationApi.model.IndConvListObject;
import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.google.gson.*;
import com.bgdev.out.backend.conversationApi.model.Conversation;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GcmIntentService extends IntentService {

    public GcmIntentService() {
        super("GcmIntentService");
    }

    public static boolean bSendNotification = true;

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        //Get the stored preferences
        if (Globals.settings==null) Globals.settings = getSharedPreferences(Globals.PREFERNCES, 0);
        Globals.USER_PROF_ID = Globals.settings.getString(Globals.USER_PROF_ID_KEY,"");
        Globals.USER_NAME = Globals.settings.getString(Globals.USER_NAME_KEY, "");
        Globals.bProfileLoaded = Globals.settings.getBoolean(Globals.LOGGED_IN_KEY, false);
        Globals.UNIQ_ID = Globals.settings.getLong(Globals.PROPERTY_UNIQ_ID, 0);
        Globals.DEVICE_REG_ID = Globals.settings.getString(Globals.PROPERTY_REG_ID, "");
        Globals.NOTIFY_OBJECT_GSON = Globals.settings.getString(Globals.NOTIF_OBJECT_KEY,"");

        //Get the settings preferences
        if (Globals.customSettings==null) Globals.customSettings = PreferenceManager.getDefaultSharedPreferences(this);
        Globals.customEditor = Globals.customSettings.edit();

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            // Since we're not using two way messaging, this is all we really to check for
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                //Logger.getLogger("GCM_RECEIVED").log(Level.INFO, extras.toString());

                String type = extras.getString("type");
                if (Globals.GCM_FRIEND_REQUEST_TYPE.equals(type)){
                    createFriendRequestNotification(extras.getString("fromName"),extras.getString("fromId"));
                }
                else if (Globals.GCM_MESSAGE_TYPE.equals(type)){
                    bSendNotification = true;
                    Long convId = setMessageVariables(Long.parseLong(extras.getString("convId")));
                    if (bSendNotification) createMessageNotification(extras.getString("fromName"), extras.getString("fromID"), extras.getString("message"), convId,extras.getString("profId"));
                    updateRequiredGlobals();
                }
                else if (Globals.UPDATE_WITH_DOTS_TYPE.equals(type)){
                    if (IndividualMessageActivity.mIndMessageRecView!=null && IndividualMessageActivity.mIndMessageRecView.isShown() && Globals.currentClickedConversation!=null){
                        if (Globals.currentClickedConversation.getId().equals(Long.parseLong(extras.getString("convId")))){
                            LocalBroadcastManager.getInstance(this).registerReceiver(IndividualMessageActivity.updateMessangerWithDotsReceiver, new IntentFilter(Globals.UPDATE_IND_MESSAGE_WITH_DOTS));
                            Intent sendIntent = new Intent(Globals.UPDATE_IND_MESSAGE_WITH_DOTS);
                            sendIntent.putExtra("sendUserId",extras.getString("sendUserId"));
                            sendIntent.putExtra("state",extras.getString("state"));
                            sendBroadcast(sendIntent);
                        }
                    }
                }

            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("GCMServiceOnDestory", "GCM Service OnDestroy");
    }

    public void createMessageNotification(String fromName, String fromId, String message, Long convId, String fromProfId){
        //Gets the current NotificationObject
        Gson gson = new Gson();
        NotificationObject object = gson.fromJson(Globals.NOTIFY_OBJECT_GSON,NotificationObject.class);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setAutoCancel(true);
        mBuilder.setSmallIcon(R.drawable.ic_out_notify_trans);

        //Big notification
        Bitmap bitmap = StaticConvertMethods.returnBigPicBitMap(fromProfId,1);
        if (bitmap!=null) mBuilder.setLargeIcon(bitmap);

        if (object == null) {
            object = new NotificationObject();
            mBuilder.setContentTitle(fromName);
            mBuilder.setContentText(Html.fromHtml(message));
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }
        else{
            //Build content title
            mBuilder.setContentTitle(object.getNameContentTitle(1));

            //Build contentText
            mBuilder.setContentText(object.getNameContentText(fromName));

            //Build the InboxStyle
            mBuilder.setStyle(object.buildInboxStyle(message,fromName));
        }

        //Pressed Intent
//        Intent pressedIntent = new Intent(this,IndividualMessageActivity.class);
//        if (listConvPosition!=-1) pressedIntent.putExtra("notif_key",listConvPosition+1);
//        TaskStackBuilder builder = TaskStackBuilder.create(this);
//        builder.addParentStack(IndividualMessageActivity.class);
//
//        PendingIntent pressedPendingIntent = builder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
//        mBuilder.setContentIntent(pressedPendingIntent);

        //Pressed intent
        Intent pressedIntent = new Intent(this,IndividualMessageActivity.class);
        if (convId!=null) pressedIntent.putExtra("notif_key",convId);
        PendingIntent pressedPendingIntent = PendingIntent.getActivity(this, 0, pressedIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pressedPendingIntent);

        //Delete Intent
        Intent deleteIntent = new Intent(Globals.NOTIF_DELETE_INTENT);
        mBuilder.setDeleteIntent(PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Build vibrate, ringtone, and priority
        buildNotificationCommonPieces(mBuilder);

        //Voice Action reply
        Intent replyIntent = new Intent(Globals.UPDATE_MESSAGE_FROM_WEAR_INTENT);
        if (convId!=null) replyIntent.putExtra("convPos",convId);
        PendingIntent remotePending = PendingIntent.getBroadcast(this,0,replyIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteInput remoteInput = new RemoteInput.Builder(Globals.EXTRA_VOIEC_REPLY_FROM_WEAR).setLabel(getResources().getString(R.string.reply_label)).build();
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white_48dp,getString(R.string.reply_label),remotePending).addRemoteInput(remoteInput).build();
        mBuilder.extend(new NotificationCompat.WearableExtender().addAction(action));

        //Build the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(0, mBuilder.build());

        //Save off the Object in Json
        object.addToNotifyFromList(Long.parseLong(fromId));
        object.addToNotifyContentList(message);
        object.addToNotifyTypeList(1);
        object.addToNotifyFromNameList(fromName);
        String json = gson.toJson(object);
        Globals.settings.edit().putString(Globals.NOTIF_OBJECT_KEY,json).commit();
    }

    public void createFriendRequestNotification(String fromName, String fromId){
        //Style the notification
        String msg = fromName + " has sent you a friend request";
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setAutoCancel(true);
        mBuilder.setContentTitle("Out");
        mBuilder.setContentText(msg);
        mBuilder.setSmallIcon(R.drawable.ic_out_notify_trans);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));

        //Pressed Intent
        Intent pressedIntent = new Intent(this,MainActivity.class);
        pressedIntent.putExtra("intentFrag", "invite");
        PendingIntent pressedPendingIntent = PendingIntent.getActivity(this,0,pressedIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pressedPendingIntent);

        //Accept or Reject Intents
        Intent acceptIntent = new Intent(Globals.ACCEPT_FRIEND_REQUEST_IF);
        acceptIntent.putExtra("fromId", fromId);
        Intent rejectIntent = new Intent(Globals.REJECT_FRIEND_REQUEST_IF);
        rejectIntent.putExtra("from", Long.valueOf(fromId).longValue());

        mBuilder.addAction(R.drawable.ic_action_good, "Accept", PendingIntent.getBroadcast(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        mBuilder.addAction(R.drawable.ic_action_bad, "Reject", PendingIntent.getBroadcast(this, 0, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(0,mBuilder.build());
    }


    public Long setMessageVariables(Long convId){
        if (MyAsyncTasks.conversationApi==null) MyAsyncTasks.registerConversationService();
        try{
            if (Globals.listOfConversations==null) Globals.listOfConversations = MyAsyncTasks.conversationApi.queryUsersConversations(Globals.UNIQ_ID).execute().getItems();
            AllConversationList temp = MyAsyncTasks.conversationApi.queryUsersConversationsMap(Globals.UNIQ_ID).execute();
            for (int i = 0; i <temp.getMessageList().size();i++){
                IndConvListObject obj = temp.getMessageList().get(i);
                Globals.allConvIndMessMap.put(obj.getConversationid(),obj.getListOfIndMessages());
            }

            boolean bFound = false;
            for (int i = 0; i < Globals.listOfConversations.size(); i++){
                Conversation conversation = Globals.listOfConversations.get(i);
                if (conversation.getId().equals(convId)){
                    Globals.currentClickedConversation = conversation;
                    bFound = true;
                }
                if (bFound) break;
            }

            //If we don't find it the first time, refresh and try again
            if (!bFound){
                Globals.listOfConversations = MyAsyncTasks.conversationApi.queryUsersConversations(Globals.UNIQ_ID).execute().getItems();
                for (int i = 0; i < Globals.listOfConversations.size(); i++){
                    Conversation conversation = Globals.listOfConversations.get(i);
                    if (conversation.getId().equals(convId)){
                        Globals.currentClickedConversation = conversation;
                        bFound = true;
                    }
                    if (bFound) break;
                }
            }

            if (IndividualMessageActivity.mIndMessageRecView!=null && IndividualMessageActivity.mIndMessageRecView.isShown() && Globals.currentClickedConversation!=null){
                IndividualMessageActivity.listOfMessages = Globals.allConvIndMessMap.get(Globals.currentClickedConversation.getId());
                if (IndividualMessageActivity.mIndMessageRecViewAdap!=null) {
                    LocalBroadcastManager.getInstance(this).registerReceiver(IndividualMessageActivity.updateMessangerWithContentReceiver, new IntentFilter(Globals.UPDATE_IND_MESSAGE_WITH_CONTENT));
                    Intent sendIntent = new Intent(Globals.UPDATE_IND_MESSAGE_WITH_CONTENT);
                    sendBroadcast(sendIntent);
                    bSendNotification = false;
                }
            }
        }
        catch (IOException i){i.printStackTrace();}
        return Globals.currentClickedConversation.getId();
    }

    public void updateRequiredGlobals(){
        if (MyAsyncTasks.userRecordApi==null) MyAsyncTasks.registerUserRecord();
        try{
            if (Globals.userFriendList==null) Globals.userFriendList= MyAsyncTasks.userRecordApi.queryFriendsUserRecs(Globals.UNIQ_ID).execute().getItems();
            if (Globals.myUser==null) Globals.myUser = MyAsyncTasks.userRecordApi.queryUser(Globals.UNIQ_ID).execute();
        }
        catch (IOException i){
            i.printStackTrace();
        }
    }

    public void buildNotificationCommonPieces(NotificationCompat.Builder builder){
        //Vibration
        boolean vibrate = Globals.customSettings.getBoolean("notify_vibrate",false);
        if (vibrate) builder.setVibrate(new long[]{0,300});

        //Ringtone
        String strRingTone = Globals.customSettings.getString("notify_ring","");
        Uri ringtoneURI = Uri.parse(strRingTone);
        builder.setSound(ringtoneURI);

        //Priority
        if (Build.VERSION.SDK_INT >= 21){
            if (Globals.customSettings.getBoolean("notify_priority",false)) builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
    }

    public static class AcceptFriendRequestBroadcastReceiver extends BroadcastReceiver{
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context,"Accepted",Toast.LENGTH_SHORT).show();
            }
    }

    public static class RejectFriendRequestBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context,"Rejected",Toast.LENGTH_SHORT).show();
            new MyAsyncTasks.RejectFriendRequestAsyncTask(intent.getLongExtra("from",0),Globals.UNIQ_ID).execute();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.cancel(0);
        }
    }

    public static class NotifyDeleteBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Gson gson = new Gson();
            NotificationObject object = gson.fromJson(Globals.NOTIFY_OBJECT_GSON, NotificationObject.class);
            if (object!=null){
                object.clearAllLists();
            }
            Globals.settings.edit().putString(Globals.NOTIF_OBJECT_KEY,"").commit();
        }
    }

    public static class UpdateFromWearBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String message=null;
            Long convId = intent.getExtras().getLong("convPos");
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput!=null ) message = remoteInput.getCharSequence(Globals.EXTRA_VOIEC_REPLY_FROM_WEAR).toString();
            if (convId!=0L && message!=null) {
                if (Globals.listOfConversations==null || Globals.listOfConversations.isEmpty()){
                    try{
                        if (Globals.UNIQ_ID==null || Globals.UNIQ_ID==0){
                            Globals.UNIQ_ID=context.getSharedPreferences(Globals.PREFERNCES, 0).getLong(Globals.PROPERTY_UNIQ_ID, 0);
                        }
                        Globals.listOfConversations = MyAsyncTasks.conversationApi.queryUsersConversations(Globals.UNIQ_ID).execute().getItems();
                    }
                    catch (IOException i){i.printStackTrace();}
                }

                boolean bFound = false;
                for (int i = 0; i < Globals.listOfConversations.size(); i++){
                    Conversation conversation = Globals.listOfConversations.get(i);
                    if (conversation.getId().equals(convId)){
                        Globals.currentClickedConversation = conversation;
                        bFound = true;
                    }
                    if (bFound) break;
                }
                if (bFound) new MyAsyncTasks.SendIndMessageAsyncTask(Globals.currentClickedConversation,message).execute();
            }

            Gson gson = new Gson();
            NotificationObject object = gson.fromJson(Globals.NOTIFY_OBJECT_GSON, NotificationObject.class);
            if (object!=null){
                object.clearAllLists();
            }
            Globals.settings.edit().putString(Globals.NOTIF_OBJECT_KEY,"").commit();
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.cancel(0);
        }
    }

    public static class NotificationObject{
        List<Integer> notifTypesList = new ArrayList<>();
        List<Long> notifFromList = new ArrayList<>();
        List<String> notifContentList = new ArrayList<>();
        List<String> notifFromNameList = new ArrayList<>();

        public NotificationObject(){}

        public void addToNotifyTypeList(int type){
            notifTypesList.add(0,type);
        }

        public void addToNotifyFromList(Long fromId){
            notifFromList.add(0,fromId);
        }

        public void addToNotifyContentList(String content){
            notifContentList.add(0,content);
        }

        public void addToNotifyFromNameList(String name){
            notifFromNameList.add(0,name);
        }

        public void clearAllLists(){
            notifTypesList.clear();
            notifFromList.clear();
            notifContentList.clear();
            notifFromNameList.clear();
        }

        public List<Integer> getNotifTypesList() {
            return notifTypesList;
        }

        public List<Long> getNotifFromList() {
            return notifFromList;
        }

        public List<String> getNotifContentList() {
            return notifContentList;
        }

        public List<String> getNotifFromNameList() {
            return notifFromNameList;
        }

        public int getTotalListTypes(int currType){
            int returnValue;
            int sum=0;
            int size = notifTypesList.size() + 1;
            for (int i = 0; i < notifTypesList.size();i++){
                 sum = sum + notifTypesList.get(i);
            }
            sum = sum +currType;

            if (sum/size==1) returnValue=1;    //Every single one was a message
            else if (sum/size==2) returnValue = 2;     //EVery single one was an invite
            else returnValue=3;     //Combo of invites and messages

            return returnValue;
        }

        public String getNameContentText(String currName){
            String text=currName + ", ";
            Set<String> uniqNameSet = new HashSet<>(getNotifFromNameList());

            for (String name : uniqNameSet){
                if (!name.equals(currName)) text = text + name + ", ";
            }
            text = text.substring(0,text.length()-2);
            return text;
        }

        public String getNameContentTitle(int currType){
            int type = getTotalListTypes(currType);
            String contentTitle;

            if (type==1) contentTitle = notifTypesList.size()+1 + " new messages";
            else if (type==2) contentTitle = notifTypesList.size()+1 + " new invites";
            else contentTitle = notifTypesList.size()+1 + " new notifications";

            return contentTitle;
        }

        public NotificationCompat.InboxStyle buildInboxStyle(String currFromMess,String fromName){
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            String firstLine = fromName + "   " + currFromMess;
            inboxStyle.addLine(firstLine);
            for (int i = 0; i < notifContentList.size();i++){
                String line = notifFromNameList.get(i) + "   " + notifContentList.get(i);
                inboxStyle.addLine(line);
            }
            return inboxStyle;
        }
    }
}

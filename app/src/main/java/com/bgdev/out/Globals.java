package com.bgdev.out;

import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.bgdev.out.backend.conversationApi.model.AllConversationList;
import com.bgdev.out.backend.conversationApi.model.Conversation;
import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.bgdev.out.backend.statusUpRecordApi.model.StatusUpRecord;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Globals {
    public static final String LOGIN_TAG = "LoginActivity";

    public static boolean bProfileLoaded = false;
    public static boolean bShowLoginActivity=true;
    public static boolean bToolbarHidden = false;

    //Keys
    public static final String PREFERNCES = "prefs";
    public static final String LOGGED_IN_KEY = "loggedIn";
    public static final String DONT_BACK_KEY="noback";
    public static final String USER_PROF_ID_KEY = "prof_id";
    public static final String USER_NAME_KEY="user_name_key";
    public static final String GCM_FRIEND_REQUEST_TYPE = "friendRequest";
    public static final String GCM_MESSAGE_TYPE = "message";
    public static final String INTENT_HIT_KEY ="frag_intent";
    public static final String UPDATE_WITH_DOTS_TYPE = "typing";
    public static final String NOTIF_OBJECT_KEY = "notify_obj";

    public static String USER_PROF_ID="";
    public static String USER_NAME="";
    public static String NOTIFY_OBJECT_GSON="";
    //Lists
    public static List<UserRecord> userFriendList;
    public static List<UserRecord> allUserList;
    public static List<UserRecord> friendRequestLists;
    public static List<Conversation> listOfConversations;
    public static List<StatusUpRecord> statusUpRecordList;
    public static HashMap<Long,List<IndMessage>> allConvIndMessMap = new HashMap<>();

    public static UserRecord myUser;

    //Static references to be used
    public static Conversation currentClickedConversation;

    //Cloud Stuff
    public static String GCM_SENDER_ID="313015714436";
    public static String APP_ENGINE_APP_ID = "vast-node-812";
    public static String PROPERTY_UNIQ_ID ="uniq_id";
    public static String PROPERTY_REG_ID="reg_id";
    public static Long UNIQ_ID;
    public static String DEVICE_REG_ID="";

    //Notification Stuff
    public static String ACCEPT_FRIEND_REQUEST_IF = "com.bgdev.out.acceptFriendRequest";
    public static String REJECT_FRIEND_REQUEST_IF = "com.bgdev.out.denyFriendRequest";
    public static String NOTIF_DELETE_INTENT = "com.bgdev.out.notify_delete";
    public static String UPDATE_IND_MESSAGE_WITH_DOTS = "com.bgdev.out.updateIndMessageWithDots";
    public static String UPDATE_IND_MESSAGE_WITH_CONTENT = "com.bgdev.out.updateIndMessageWithContent";
    public static String SERVER_UPDATE_BROADCAST = "com.bgdev.out.serverUpdate";
    public static String EXTRA_VOIEC_REPLY_FROM_WEAR = "extra_voice_reply";
    public static String UPDATE_MESSAGE_FROM_WEAR_INTENT = "com.bgdev.out.updateFromWear";

    public static SharedPreferences settings;
    public static SharedPreferences.Editor editor;
    public static SharedPreferences customSettings;
    public static SharedPreferences.Editor customEditor;

    public static SharedPreferences.OnSharedPreferenceChangeListener listener;
    public static Bitmap profPicBitMap;
    public static int numberOfStatusToLoad;

}


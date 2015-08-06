package com.bgdev.out;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.facebook.widget.ProfilePictureView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;


public class IndividualMessageActivity extends AppCompatActivity{
    static RecyclerView mIndMessageRecView;
    static RecyclerView.Adapter mIndMessageRecViewAdap;
    static List<IndMessage> listOfMessages = new ArrayList<>();
    Toolbar mToolbar;
    static EditText editText;
    int position = 0;
    static ImageButton sendButton;
    boolean bSentMessage=false;

    static UpdateMessangerWithDotsReceiver updateMessangerWithDotsReceiver;
    static UpdateMessangerWithContentReceiver updateMessangerWithContentReceiver;
    InputMethodManager inputMethodManager;

    static Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_message);
        context = this;
        Bundle bundle = getIntent().getExtras();
        if (bundle!=null) {
            Long normKey = bundle.getLong("key");
            Long notifKey = bundle.getLong("notif_key");
            if (normKey>0) {
                listOfMessages = Globals.allConvIndMessMap.get(normKey);
                Globals.settings.edit().putString(Globals.NOTIF_OBJECT_KEY,"").commit();
            }
            else if (notifKey>0) {
                listOfMessages = Globals.allConvIndMessMap.get(notifKey);
                Gson gson = new Gson();
                GcmIntentService.NotificationObject object = gson.fromJson(Globals.NOTIFY_OBJECT_GSON, GcmIntentService.NotificationObject.class);
                if (object!=null){
                    object.clearAllLists();
                }
                Globals.settings.edit().putString(Globals.NOTIF_OBJECT_KEY,"").commit();
            }
        }

        //Set the title of the Activity Bar to the list of names in the conversation
        String title = "";
        int gSize = Globals.currentClickedConversation.getUserIdsList().size();
        for (int i=0; i < gSize; i++){
            Long l = Globals.currentClickedConversation.getUserIdsList().get(i);
            if (!l.equals(Globals.UNIQ_ID)){
                if (gSize>2){
                    title = title + StaticConvertMethods.findFriendUserRecordFromID(l).getUserName() + ", ";
                }
                else{
                    title = StaticConvertMethods.findFriendUserRecordFromID(l).getUserName();
                }
            }
        }
        if (title.endsWith(", ")) title = title.substring(0,title.length()-2);
        setTitle(title);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mIndMessageRecView = (RecyclerView) findViewById(R.id.inv_message_recycler_view);

        //new MyAsyncTasks.QueryIndMessageInConvAsyncTask(Globals.currentClickedConversation,this).execute();

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mIndMessageRecView.setHasFixedSize(true);

        //Fill in from the bottom
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        mIndMessageRecView.setLayoutManager(manager);

        mIndMessageRecViewAdap = new InvMessageRecyclerViewAdapter(listOfMessages);
        mIndMessageRecView.setAdapter(mIndMessageRecViewAdap);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        editText = (EditText) findViewById(R.id.edit_text);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listOfMessages.isEmpty()) position = 0;
                else position = listOfMessages.size() - 1;

                mIndMessageRecView.smoothScrollToPosition(position);
            }
        });

        //This is for the key codes on the keyboard with the send button
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode==KeyEvent.KEYCODE_ENTER && event.getAction()==(KeyEvent.ACTION_DOWN)){
                    if (editText.getText()!=null && !editText.getText().toString().equals("")){
                        sendMessage();
                    }
                    return true;
                }
                return false;
            }
        });

        bSentMessage = false;
        editText.addTextChangedListener(new TextWatcher() {
            boolean bSentTickler = false;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0 && !bSentTickler){
                    bSentTickler =true;
                    new MyAsyncTasks.SendTypingUpdateAsyncTask(Globals.currentClickedConversation,"show").execute();
                    bSentMessage=false;
                }
                else if (s.length() == 0 && bSentTickler){
                    bSentTickler =false;
                    if (!bSentMessage) new MyAsyncTasks.SendTypingUpdateAsyncTask(Globals.currentClickedConversation,"hide").execute();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
        });
        sendButton = (ImageButton) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText()!=null && !editText.getText().toString().equals("")){
                    sendMessage();
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (Globals.settings==null) Globals.settings = getSharedPreferences(Globals.PREFERNCES, 0);
        Globals.editor = Globals.settings.edit();
        Globals.editor.putString(Globals.INTENT_HIT_KEY,"mess").commit();

        super.onBackPressed();
    }

    public void sendMessage(){
        IndMessage msg = new IndMessage();
        String message = editText.getText().toString();
        msg.setMessageContent(message);
        msg.setMessageSentFrom(Globals.UNIQ_ID);
        msg.setMessageTimeStamp(System.currentTimeMillis());
        msg.setUserProfId(Globals.USER_PROF_ID);
        listOfMessages.add(listOfMessages.size(), msg);
        mIndMessageRecViewAdap.notifyDataSetChanged();

        bSentMessage=true;
        editText.setText("");
        position = listOfMessages.size()-1;

        mIndMessageRecView.scrollToPosition(position);

        new MyAsyncTasks.SendIndMessageAsyncTask(Globals.currentClickedConversation,message).execute();
    }

    public static class InvMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<IndMessage> mList = new ArrayList<>();

        // Provide a suitable constructor (depends on the kind of dataset)
        public InvMessageRecyclerViewAdapter(List<IndMessage> list) {
            mList = list;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            ProfilePictureView profilePictureView;
            TextView contentTextView,timeStampTextView;
            public ViewHolder(View v) {
                super(v);
                profilePictureView = (ProfilePictureView) v.findViewById(R.id.inv_message_profile_picture);
                contentTextView = (TextView) v.findViewById(R.id.inv_message_content);
                timeStampTextView = (TextView) v.findViewById(R.id.inv_message_timestamp);
            }
            public ViewHolder(View v,int type) {
                super(v);
                profilePictureView = (ProfilePictureView) v.findViewById(R.id.inv_message_profile_picture);
                contentTextView = (TextView) v.findViewById(R.id.inv_message_content);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType==0) {
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_inv_message_me_row, null);
                return new ViewHolder(rowLayoutView);
            }
            else if (viewType==1){
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_inv_message_notme_row, null);
                return new ViewHolder(rowLayoutView);
            }
            else {
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_inv_message_typing, null);
                return new ViewHolder(rowLayoutView,2);
            }
        }

        @Override
        public int getItemViewType(int position) {
            Long from = mList.get(position).getMessageSentFrom();
            Long timeStamp = mList.get(position).getMessageTimeStamp();
            if (timeStamp.equals(1L)) return 2;
            if (from.equals(Globals.UNIQ_ID)) return 0;
            else return 1;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type <2) {
                ((ViewHolder) holder).timeStampTextView.setText(StaticConvertMethods.convertLongDiffToMessageStamp(mList.get(position).getMessageTimeStamp()));
                ((ViewHolder) holder).profilePictureView.setProfileId(mList.get(position).getUserProfId());
                ((ViewHolder) holder).contentTextView.setText(mList.get(position).getMessageContent());
            }
            else{
                ((ViewHolder) holder).profilePictureView.setProfileId(mList.get(position).getUserProfId());
                ((ViewHolder) holder).contentTextView.setText(mList.get(position).getMessageContent());
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mList!=null)return mList.size();
            else return 0;
        }
    }

    public static class UpdateMessangerWithDotsReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getExtras().getString("state");
            Long userId = Long.parseLong(intent.getExtras().getString("sendUserId"));
            if (state.equals("show")) {
                IndMessage bubble = new IndMessage();
                bubble.setUserProfId(StaticConvertMethods.findFriendUserRecordFromID(userId).getUserProfId());
                bubble.setMessageTimeStamp(1L);
                bubble.setMessageContent("...");
                listOfMessages.add(listOfMessages.size(), bubble);
                mIndMessageRecViewAdap = new InvMessageRecyclerViewAdapter(listOfMessages);
                mIndMessageRecView.setAdapter(mIndMessageRecViewAdap);
            }
            else if (state.equals("hide")) {
                if (listOfMessages.get(listOfMessages.size()-1).getMessageTimeStamp().equals(1L)){
                    listOfMessages.remove(listOfMessages.size()-1);
                    mIndMessageRecViewAdap = new InvMessageRecyclerViewAdapter(listOfMessages);
                    mIndMessageRecView.setAdapter(mIndMessageRecViewAdap);
                }
            }
        }
    }

    public static class UpdateMessangerWithContentReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            mIndMessageRecViewAdap = new InvMessageRecyclerViewAdapter(listOfMessages);
            mIndMessageRecView.setAdapter(mIndMessageRecViewAdap);
        }
    }
}

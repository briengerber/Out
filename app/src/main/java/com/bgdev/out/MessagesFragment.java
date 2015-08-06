package com.bgdev.out;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bgdev.out.backend.conversationApi.model.Conversation;
import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;

import java.util.List;


public class MessagesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,AsyncDelegate{
    int drawerPosition = 3;
    static RecyclerView mMessagesRecyclerView;
    static RecyclerView.Adapter mMessagesAdapter;
    static List<Conversation> mConvList;
    static SwipeRefreshLayout swipeRefreshLayout;
    static Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View messages = inflater.inflate(R.layout.fragment_messages, container, false);
        context = getActivity();

        mConvList = Globals.listOfConversations;
        swipeRefreshLayout = (SwipeRefreshLayout) messages.findViewById(R.id.mess_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light,android.R.color.holo_orange_light,android.R.color.holo_red_light,android.R.color.holo_purple);

        mMessagesRecyclerView = (RecyclerView) messages.findViewById(R.id.messages_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mMessagesRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // specify an adapter (see also next example)
        if (mMessagesAdapter==null) {
            mMessagesAdapter = new MessagesRecyclerViewAdapter(mConvList);
            mMessagesRecyclerView.setAdapter(mMessagesAdapter);
        }
        else {
            mMessagesRecyclerView.setAdapter(mMessagesAdapter);
            mMessagesAdapter.notifyDataSetChanged();
        }
        return messages;
    }

    @Override
    public void onRefresh() {
        new MyAsyncTasks.QueryActiveConversationsAsyncTask(true,this).execute();
    }

    @Override
    public void asyncComplete(boolean success) {
        if (success) {
            mConvList = Globals.listOfConversations;
            mMessagesAdapter = new MessagesRecyclerViewAdapter(mConvList);
            mMessagesRecyclerView.setAdapter(mMessagesAdapter);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void asyncCompleteMessage(boolean success, List<IndMessage> list) {
        //not needed
    }

    public class MessagesRecyclerViewAdapter extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.ViewHolder> implements RecyclerView.OnClickListener{

        private List<Conversation> mList;

        // Provide a suitable constructor (depends on the kind of dataset)
        public MessagesRecyclerViewAdapter(List<Conversation> list) {
            mList = list;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView convTimeStamp, convMessageText,convName;
            ProfilePictureView confProfPic;
            public ViewHolder(View v) {
                super(v);
                convTimeStamp = (TextView) v.findViewById(R.id.mess_time_stamp);
                convMessageText = (TextView) v.findViewById(R.id.mess_content);
                convName = (TextView) v.findViewById(R.id.mess_user_name);
                confProfPic = (ProfilePictureView) v.findViewById(R.id.mess_profile_picture);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_messages_row, null);
            ViewHolder vh = new ViewHolder(rowLayoutView);
            rowLayoutView.setOnClickListener(this);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Conversation conversation = mList.get(position);
            //Create the profile picture
            String profid=null;
            boolean bFound = false;
            for (String p :conversation.getUserProfIdsList()){
                if (!p.equals(Globals.USER_PROF_ID)) {
                    profid=p;
                    bFound=true;
                }
                if (bFound) break;
            }
            holder.confProfPic.setProfileId(profid);

            //create the user name
            String userName="";
            for (int i = 0; i < conversation.getUserIdsList().size();i++) {
                Long l = conversation.getUserIdsList().get(i);
                if (!l.equals(Globals.UNIQ_ID)) {
                    UserRecord userRecord = StaticConvertMethods.findFriendUserRecordFromID(l);
                    if (userName.equals("")) userName = userRecord.getUserName();
                    else userName = userName + ", " + userRecord.getUserName();
                }
            }
            holder.convName.setText(userName);

            //create the message text
            String messageText;
            if (conversation.getMostRecentUser().equals(Globals.UNIQ_ID)) messageText= "You: " + conversation.getMostRecentText();
            else  if (conversation.getUserIdsList().size()>2) {
                messageText = StaticConvertMethods.findFriendUserRecordFromID(conversation.getMostRecentUser()).getUserName()+": " + conversation.getMostRecentText();
            }
            else {
                messageText=conversation.getMostRecentText();
            }
            holder.convMessageText.setText(messageText);

            //create the message timestamp
            holder.convTimeStamp.setText(StaticConvertMethods.convertLongDiffToMessageStamp(conversation.getMostRecentTimeStamp()));

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mList!=null)return mList.size();
            else return 0;
        }

        @Override
        public void onClick(View v) {
            int position = mMessagesRecyclerView.getChildLayoutPosition(v);
            Globals.currentClickedConversation = mList.get(position);
            Intent indMessage = new Intent(context,IndividualMessageActivity.class);
            indMessage.putExtra("key",Globals.currentClickedConversation.getId());
            startActivity(indMessage);
        }
    }

}


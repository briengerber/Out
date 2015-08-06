package com.bgdev.out;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bgdev.out.backend.statusUpRecordApi.model.StatusUpRecord;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfileFragment extends Fragment implements View.OnClickListener, UpdateStatusInterface{

    //RecyclerView Variables
    static RecyclerView mProfileRecyclerView;
    static RecyclerView.Adapter mProfileAdapter;
    static List<StatusUpRecord> mList = new ArrayList<>();
    static SwipeRefreshLayout swipeRefreshLayout;

    static UserRecord localUserRecord;
    static Long mUserID;
    static ProgressBar progressBar;
    static HashMap<Long,List<StatusUpRecord>> localMap = new HashMap<>();

    public static ProfileFragment newInstance(UserRecord userRecord, Long userID){
        ProfileFragment frag = new ProfileFragment();
        localUserRecord = userRecord;
        mUserID = userID;
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        mProfileRecyclerView = (RecyclerView) v.findViewById(R.id.my_recycler_view);
        progressBar = (ProgressBar) v.findViewById(R.id.prof_progress_bar);

        mList = localMap.get(mUserID);
        if (mList==null) {
            mList = new ArrayList<>();
            progressBar.setVisibility(View.VISIBLE);
            new MyAsyncTasks.QueryUserStatusForOneUser(mUserID,this,mList).execute();
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mProfileRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mProfileRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // specify an adapter (see also next example)
        mProfileAdapter = new ProfileRecyclerViewAdapter(mList);
        mProfileRecyclerView.setAdapter(mProfileAdapter);

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab_going_out:
                startActivityForResult(new Intent(getActivity(), GoingOutActivity.class), 1);
                break;
            case R.id.fab_undecided:
                new MyAsyncTasks.CreateOrUpdateStatusAsyncTask(getActivity(),2).execute();
                break;
            case R.id.fab_staying_in:
                new MyAsyncTasks.CreateOrUpdateStatusAsyncTask(getActivity(),1).execute();
                break;
        }
    }

    @Override
    public void update(List<StatusUpRecord> list) {
        list.add(0,new StatusUpRecord());
        mList = list;
        mProfileAdapter = new ProfileRecyclerViewAdapter(mList);
        mProfileRecyclerView.setAdapter(mProfileAdapter);
        localMap.put(mUserID,mList);
        if (progressBar.getVisibility()==View.VISIBLE) progressBar.setVisibility(View.GONE);
    }
    //Not needed here
    @Override
    public void updateFromScroll(List<StatusUpRecord> list) {

    }

    public static class ProfileRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<StatusUpRecord> mList = new ArrayList<>();

        // Provide a suitable constructor (depends on the kind of dataset)
        public ProfileRecyclerViewAdapter(List<StatusUpRecord> list) {
            mList = list;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView userNameTextView,numOfFriendsTextView;
            ProfilePictureView profilePictureView;

            public ViewHolder(View v) {
                super(v);
                userNameTextView = (TextView) v.findViewById(R.id.user_name);
                profilePictureView = (ProfilePictureView) v.findViewById(R.id.profile_picture);
                numOfFriendsTextView = (TextView) v.findViewById(R.id.num_friends_text);
            }

        }

        public class ListViewHolder extends RecyclerView.ViewHolder{
            TextView homeUserNameTextView,homeUserWhereTextView,homeUserWhoTextView,homeUserStatusTextView,homeUserStatusTimeStamp;
            ProfilePictureView homeProfilePictureView;
            public ListViewHolder(View v){
                super(v);
                homeUserNameTextView = (TextView) v.findViewById(R.id.home_user_name);
                homeUserWhereTextView = (TextView) v.findViewById(R.id.home_user_where);
                homeUserWhoTextView = (TextView) v.findViewById(R.id.home_user_who);
                homeUserStatusTextView = (TextView) v.findViewById(R.id.home_user_status);
                homeProfilePictureView = (ProfilePictureView) v.findViewById(R.id.home_profile_picture);
                homeUserStatusTimeStamp = (TextView) v.findViewById(R.id.home_user_timestamp);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position==0) return 0;
            else return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType==0) {
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_prof_head_row, null);
                return new ViewHolder(rowLayoutView);
            }else{
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_home_row, null);
                return new ListViewHolder(rowLayoutView);
            }
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position==0){
                ((ViewHolder)(holder)).profilePictureView.setProfileId(localUserRecord.getUserProfId());
                ((ViewHolder)(holder)).userNameTextView.setText(localUserRecord.getUserName());
                if (localUserRecord.getListOfFriends()!=null) ((ViewHolder)(holder)).numOfFriendsTextView.setText(Integer.toString(localUserRecord.getListOfFriends().size()));
                else ((ViewHolder)(holder)).numOfFriendsTextView.setText(Integer.toString(0));
            }
            else{
                ((ListViewHolder)(holder)).homeUserNameTextView.setText(mList.get(position).getStatusUserName());
                ((ListViewHolder)(holder)).homeProfilePictureView.setProfileId(mList.get(position).getStatusUserProfId());
                ((ListViewHolder)(holder)).homeUserStatusTextView.setText(StaticConvertMethods.convertIntStatusToString(mList.get(position).getStatus()));
                ((ListViewHolder)(holder)).homeUserStatusTimeStamp.setText(StaticConvertMethods.convertLongDiffToTimeStamp(mList.get(position).getStatusUpdateLong()));

                //This will need to be replaced. This is only showing the first person you add
                List<Long> friendIdsGoingList = mList.get(position).getListOfPeopleGoing();
                if (friendIdsGoingList!=null && !friendIdsGoingList.isEmpty()){
                    UserRecord friend = StaticConvertMethods.findFriendUserRecordFromID(friendIdsGoingList.get(0));
                    if (friend!=null) {
                        ((ListViewHolder)(holder)).homeUserWhoTextView.setText(friend.getUserName());
                    }
                }
                else{
                    ((ListViewHolder)(holder)).homeUserWhoTextView.setText("Who are they going with?");
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mList!=null)return mList.size();
            else return 0;
        }

    }

}

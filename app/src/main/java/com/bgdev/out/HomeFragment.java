package com.bgdev.out;


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.design.widget.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bgdev.out.backend.conversationApi.model.IndMessage;
import com.bgdev.out.backend.statusUpRecordApi.model.StatusUpRecord;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener,AsyncDelegate,UpdateStatusInterface{

    int drawerPosition = 1;
    static RecyclerView mHomeRecyclerView;
    static RecyclerView.Adapter mHomeAdapter;
    static SwipeRefreshLayout swipeRefreshLayout;
    static RelativeLayout homeLayout;
    static LinearLayoutManager mLayoutManager;

    static List<StatusUpRecord> statusUpRecordList = new ArrayList<>();
    static ProgressBar progressBar;

    static FloatingActionButton menuButton,goingOutButton,unDecidedButton,stayingInButton;
    static boolean bMenuOpen;
    Drawable backgrounds[] = new Drawable[2];
    TransitionDrawable transitionDrawable;
    static float topTran=-475, midTran=-325, botTran=-175;

    static Context context;
    static TextView hideText;
    static UpdateStatusInterface updateStatusInterface;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View home = inflater.inflate(R.layout.fragment_home, container, false);

        context = getActivity();
        updateStatusInterface = this;
        //set up the progress bar
        progressBar = (ProgressBar) home.findViewById(R.id.home_progress_bar);

        mHomeRecyclerView = (RecyclerView) home.findViewById(R.id.home_recycler_view);

        swipeRefreshLayout = (SwipeRefreshLayout) home.findViewById(R.id.home_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_orange_light, android.R.color.holo_red_light, android.R.color.holo_purple);
        swipeRefreshLayout.setProgressViewOffset(false,50,170);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mHomeRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mHomeRecyclerView.setLayoutManager(mLayoutManager);

        statusUpRecordList = Globals.statusUpRecordList;
        mHomeAdapter = new HomeRecyclerViewAdapter(statusUpRecordList);
        mHomeRecyclerView.setAdapter(mHomeAdapter);
        mHomeRecyclerView.setOnScrollListener(new HomeOnScrollChangeListener());
        //Show the progress bar is not loaded
        if (statusUpRecordList==null || statusUpRecordList.isEmpty()) progressBar.setVisibility(View.VISIBLE);

        //Floating Action buttons
        menuButton = (FloatingActionButton) home.findViewById(R.id.fab);
        menuButton.setOnClickListener(this);

        bMenuOpen=false; //Default to menu closed
        goingOutButton = (FloatingActionButton) home.findViewById(R.id.fab_going_out);
        goingOutButton.setOnClickListener(this);

        unDecidedButton = (FloatingActionButton) home.findViewById(R.id.fab_undecided);
        unDecidedButton.setOnClickListener(this);

        stayingInButton = (FloatingActionButton) home.findViewById(R.id.fab_staying_in);
        stayingInButton.setOnClickListener(this);
        return home;
    }

    @Override
    public void onRefresh() {
        new MyAsyncTasks.QueryStatusUpdateAsyncTask(true,this).execute();
    }

    @Override
    public void asyncComplete(boolean success) {
        if (success) {
            statusUpRecordList = Globals.statusUpRecordList;
            mHomeAdapter = new HomeRecyclerViewAdapter(statusUpRecordList);
            mHomeRecyclerView.setAdapter(mHomeAdapter);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    //Updates from the scrolling bellow
    @Override
    public void updateFromScroll(List<StatusUpRecord> list) {
        if (list !=null) {
            for (int i = 0; i < list.size(); i++) {
                if (!Globals.statusUpRecordList.contains(list.get(i))){
                    Globals.statusUpRecordList.add(list.get(i));
                    mHomeAdapter.notifyItemInserted(Globals.statusUpRecordList.size());
                }
            }

            statusUpRecordList = Globals.statusUpRecordList;
        }
    }

    @Override
    public void update(List<StatusUpRecord> list) {
        statusUpRecordList=list;
        mHomeAdapter = new HomeRecyclerViewAdapter(statusUpRecordList);
        mHomeRecyclerView.setAdapter(mHomeAdapter);
    }

    //Swype refresh
    @Override
    public void asyncCompleteMessage(boolean success, List<IndMessage> list) {
        //not needed
    }


    //On Click for the button presses
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                bMenuOpen = !bMenuOpen;
                if (bMenuOpen) {
                    menuButton.animate().rotationBy(135).setInterpolator(new AccelerateInterpolator(0.25f)).start();
                    goingOutButton.animate().translationY(topTran).setInterpolator(new AccelerateInterpolator(0.5f)).start();
                    goingOutButton.setVisibility(View.VISIBLE);

                    unDecidedButton.animate().translationY(midTran).setInterpolator(new AccelerateInterpolator(0.5f)).start();
                    unDecidedButton.setVisibility(View.VISIBLE);

                    stayingInButton.animate().translationY(botTran).setInterpolator(new AccelerateInterpolator(0.5f)).start();
                    stayingInButton.setVisibility(View.VISIBLE);
                }
                else {
                    collapseAllOpenButtons();
                }
                break;
            case R.id.fab_going_out:
                collapseAllOpenButtons();
                bMenuOpen=false;
                Toast.makeText(getActivity(),"Going Out",Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(getActivity(), GoingOutActivity.class), 1);
                break;
            case R.id.fab_undecided:
                collapseAllOpenButtons();
                bMenuOpen=false;
                Toast.makeText(getActivity(), "Undecided", Toast.LENGTH_SHORT).show();
                new MyAsyncTasks.CreateOrUpdateStatusAsyncTask(getActivity(),2).execute();
                break;
            case R.id.fab_staying_in:
                collapseAllOpenButtons();
                bMenuOpen=false;
                Toast.makeText(getActivity(),"Staying in",Toast.LENGTH_SHORT).show();
                new MyAsyncTasks.CreateOrUpdateStatusAsyncTask(getActivity(),1).execute();
                break;
        }
    }

    public static void collapseAllOpenButtons(){
        menuButton.animate().rotationBy(-135).setInterpolator(new AccelerateInterpolator(0.25f)).start();
        ViewPropertyAnimator goingOutAnimator = goingOutButton.animate();
        goingOutAnimator.withEndAction(new Runnable() {
            @Override
            public void run() {
                goingOutButton.setVisibility(View.GONE);
            }
        });
        goingOutAnimator.translationY(0).setInterpolator(new AnticipateInterpolator(0.5f)).start();

        ViewPropertyAnimator undecAnimator = unDecidedButton.animate();
        undecAnimator.withEndAction(new Runnable() {
            @Override
            public void run() {
                unDecidedButton.setVisibility(View.GONE);
            }
        });
        undecAnimator.translationY(0).setInterpolator(new AnticipateInterpolator(0.5f)).start();

        ViewPropertyAnimator stayingInAnimator = stayingInButton.animate();
        stayingInAnimator.withEndAction(new Runnable() {
            @Override
            public void run() {
                stayingInButton.setVisibility(View.GONE);
            }
        });
        stayingInAnimator.translationY(0).setInterpolator(new AnticipateInterpolator(0.5f)).start();
    }

    public static class HomeRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        public static List<StatusUpRecord> mList;

        // Provide a suitable constructor (depends on the kind of dataset)
        public HomeRecyclerViewAdapter(List<StatusUpRecord> list) {
            mList = list;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            TextView homeUserNameTextView,homeUserWhereTextView,homeUserWhoTextView,homeUserStatusTextView,homeUserStatusTimeStamp,homeUserDescription;
            ProfilePictureView homeProfilePictureView;

            public MyViewHolder(View v) {
                super(v);
                homeUserNameTextView = (TextView) v.findViewById(R.id.home_user_name);
                homeUserWhereTextView = (TextView) v.findViewById(R.id.home_user_where);
                homeUserWhoTextView = (TextView) v.findViewById(R.id.home_user_who);
                homeUserStatusTextView = (TextView) v.findViewById(R.id.home_user_status);
                homeProfilePictureView = (ProfilePictureView) v.findViewById(R.id.home_profile_picture);
                homeUserStatusTimeStamp = (TextView) v.findViewById(R.id.home_user_timestamp);
                homeUserDescription = (TextView) v.findViewById(R.id.home_user_description);
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder{
            public HeaderViewHolder(View v){super(v);}
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType==1) {
                View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_home_row,parent, false);
                return new MyViewHolder(rowLayoutView);
            }
            else{
                View holderView = LayoutInflater.from(parent.getContext()).inflate(R.layout.topheader,parent,false);
                return new HeaderViewHolder(holderView);
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            StatusUpRecord statusUpRecord;
            if (position>0) {
                position = position-1;
                statusUpRecord = mList.get(position);
                MyViewHolder myViewHolder = ((MyViewHolder)holder);
                myViewHolder.homeUserNameTextView.setText(statusUpRecord.getStatusUserName());
                myViewHolder.homeProfilePictureView.setProfileId(statusUpRecord.getStatusUserProfId());
                myViewHolder.homeUserStatusTextView.setText(StaticConvertMethods.convertIntStatusToString(statusUpRecord.getStatus()));
                myViewHolder.homeUserStatusTimeStamp.setText(StaticConvertMethods.convertLongDiffToTimeStamp(statusUpRecord.getStatusUpdateLong()));

                //This will need to be replaced. This is only showing the first person you add
                List<Long> friendIdsGoingList = statusUpRecord.getListOfPeopleGoing();
                if (friendIdsGoingList != null && !friendIdsGoingList.isEmpty()) {
                    UserRecord friend = StaticConvertMethods.findFriendUserRecordFromID(friendIdsGoingList.get(0));
                    if (friend != null) {
                        myViewHolder.homeUserWhoTextView.setText(friend.getUserName());
                    }
                } else {
                    myViewHolder.homeUserWhoTextView.setText("Who are they going with?");
                }

                String placeName = statusUpRecord.getStatusPlaceName();
                if (placeName != null) {
                    if (placeName.equals(mList.get(position).getStatusPlaceName())) {
                        myViewHolder.homeUserWhereTextView.setText(placeName);
                    }
                } else {
                    myViewHolder.homeUserWhereTextView.setText("Where are they going?");
                }

                String desc = statusUpRecord.getStatusDescription();
                if (desc != null) myViewHolder.homeUserDescription.setText(desc);
                else myViewHolder.homeUserDescription.setText("Description");

                myViewHolder.homeProfilePictureView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StatusUpRecord clicked = mList.get(holder.getLayoutPosition()-1);
                        ProfileFragment frag = ProfileFragment.newInstance(StaticConvertMethods.findFriendUserRecordFromID(clicked.getUserId()), clicked.getUserId());
                        FragmentTransaction fragmentTransaction = MainActivity.fm.beginTransaction();
                        fragmentTransaction.replace(R.id.content_frame, frag);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                        MainActivity.fm.executePendingTransactions();
                    }
                });
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return getBaseItemCount()+1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position==0) return 0;
            else return 1;
        }

        public int getBaseItemCount(){
            return mList==null ? 0 : mList.size();
        }
    }

    public static class HomeOnScrollChangeListener extends RecyclerView.OnScrollListener{
        Toolbar mToolbar;
        int prevCount = 0;
        boolean bLoaded = false;

        public HomeOnScrollChangeListener(){
            mToolbar = MainActivity.getToolbar();
            Globals.bToolbarHidden = false;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (dy >5 && !Globals.bToolbarHidden) {
                menuButton.animate().translationY(200f).setInterpolator(new AccelerateInterpolator()).start();
                mToolbar.animate().translationY(-mToolbar.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                if (bMenuOpen){
                    goingOutButton.animate().translationY(200f).setInterpolator(new AccelerateInterpolator()).start();
                    unDecidedButton.animate().translationY(200f).setInterpolator(new AccelerateInterpolator()).start();
                    stayingInButton.animate().translationY(200f).setInterpolator(new AccelerateInterpolator()).start();
                }
                Globals.bToolbarHidden = true;
            }
            else if (dy < -5 && Globals.bToolbarHidden){
                menuButton.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                mToolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                if (bMenuOpen) {
                    goingOutButton.animate().translationY(topTran).setInterpolator(new AccelerateInterpolator()).start();
                    unDecidedButton.animate().translationY(midTran).setInterpolator(new AccelerateInterpolator()).start();
                    stayingInButton.animate().translationY(botTran).setInterpolator(new AccelerateInterpolator()).start();
                }
                Globals.bToolbarHidden= false;
            }

            if (dy > 0){
                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                if (visibleItemCount+pastVisibleItems==totalItemCount && !bLoaded){
                    new MyAsyncTasks.QueryStatusUpdateFromScroll(totalItemCount-1,updateStatusInterface).execute();
                    bLoaded = true;
                }

                if (prevCount!= 0 && prevCount!=totalItemCount) bLoaded = false;
                prevCount = totalItemCount;
            }
        }
    }

}

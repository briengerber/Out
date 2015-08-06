package com.bgdev.out;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class InvitesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    static RecyclerView mInvitesRecyclerView;
    static RecyclerView.Adapter mInvitesAdapter;
    static SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View invites = inflater.inflate(R.layout.fragment_invites, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout) invites.findViewById(R.id.invite_swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light,android.R.color.holo_orange_light,android.R.color.holo_red_light,android.R.color.holo_purple);

        mInvitesRecyclerView = (RecyclerView) invites.findViewById(R.id.invites_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mInvitesRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mInvitesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // specify an adapter (see also next example)
        mInvitesAdapter = new InvitesRecyclerViewAdapter(Globals.friendRequestLists);
        mInvitesRecyclerView.setAdapter(mInvitesAdapter);

        return invites;
    }


    @Override
    public void onRefresh() {new MyAsyncTasks.QueryFriendInvitesAsyncTask(true).execute();
    }

    public static class InvitesRecyclerViewAdapter extends RecyclerView.Adapter<InvitesRecyclerViewAdapter.ViewHolder>{

        private List<UserRecord> mList = new ArrayList<>();

        // Provide a suitable constructor (depends on the kind of dataset)
        public InvitesRecyclerViewAdapter(List<UserRecord> list) {
            mList = list;
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            ProfilePictureView profilePictureView;
            TextView nameTextView;

            public ViewHolder(View v) {
                super(v);
                profilePictureView = (ProfilePictureView) v.findViewById(R.id.inv_profile_picture);
                nameTextView = (TextView) v.findViewById(R.id.inv_user_name);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_invites_row, null);
            ViewHolder vh = new ViewHolder(rowLayoutView);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.profilePictureView.setProfileId(mList.get(position).getUserProfId());
            holder.nameTextView.setText(mList.get(position).getUserName());
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mList!=null) return mList.size();
            else return 0;
        }

    }

}

package com.bgdev.out;


import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bgdev.out.backend.registration.model.Friend;
import com.bgdev.out.backend.registration.model.RegistrationRecord;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SearchFragment extends android.support.v4.app.Fragment {

    int drawerPosition = 2;
    static RecyclerView mSearchRecyclerView;
    static RecyclerView.Adapter mSearchAdapter;
    static TextView searchforPeople,editText;
    static List<String> tempUserNameList = new ArrayList<>();
    public static List<UserRecord> tempUserList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View searchView = inflater.inflate(R.layout.fragment_search, container, false);

        setHasOptionsMenu(true);    //Allows for options menu in this fragment

        tempUserNameList.clear();
        tempUserList.clear();

        mSearchRecyclerView = (RecyclerView) searchView.findViewById(R.id.search_recycle_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mSearchRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // specify an adapter (see also next example)
        mSearchAdapter = new SearchRecyclerViewAdapter(tempUserList);
        mSearchRecyclerView.setAdapter(mSearchAdapter);

        searchforPeople = (TextView) searchView.findViewById(R.id.search_for_people_text);

        return searchView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Implementing ActionBar Search inside a fragment
        MenuItem item = menu.add("Search");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SearchView sv = new SearchView(getActivity());

        sv.setIconified(false);

        // modifying the text inside edittext component
        editText = (TextView) sv.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        editText.setHint("Search");
        editText.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        editText.setTextColor(getResources().getColor(R.color.actionBarTextColor));

        // implementing the listener
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                for (int i = 0; i<Globals.allUserList.size(); i++) {
                    UserRecord userRecord = Globals.allUserList.get(i);
                    String name = userRecord.getUserName();
                    boolean a = name.toLowerCase().contains(newText.toLowerCase());
                    boolean b = tempUserNameList.contains(name);
                    if (a && !b) {
                        tempUserList.add(userRecord);
                        tempUserNameList.add(name);
                    }
                    else if (!a && b) {
                        tempUserList.remove(userRecord);
                        tempUserNameList.remove(name);
                    }
                }

                if (newText.length() == 0) {
                    tempUserList.clear();
                    tempUserNameList.clear();
                }

                else {
                    searchforPeople.setVisibility(View.INVISIBLE);
                }

                Collections.sort(tempUserList,new Comparator<UserRecord>() {
                    @Override
                    public int compare(UserRecord lhs, UserRecord rhs) {
                        return lhs.getUserName().compareTo(rhs.getUserName());
                    }
                });
                mSearchAdapter.notifyDataSetChanged();
                return true;
            }
        });
        item.setActionView(sv);
    }

    public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder>{

        private List<UserRecord> mList = new ArrayList<>();
        private List<UserRecord> mAllUser = new ArrayList<>();
        private boolean bNoFriendsReturned;

        // Provide a suitable constructor (depends on the kind of dataset)
        public SearchRecyclerViewAdapter(List<UserRecord> list) {
            mList = list;
        }


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView otherUsersNameTextView;
            CardView cardView;
            ProfilePictureView profilePictureView;

            public ViewHolder(View v) {
                super(v);
                otherUsersNameTextView = (TextView) v.findViewById(R.id.other_user_name);
                cardView = (CardView) v.findViewById(R.id.card_view);
                profilePictureView = (ProfilePictureView) v.findViewById(R.id.profile_picture);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_search_row, null);
            return new ViewHolder(rowLayoutView);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.otherUsersNameTextView.setText(mList.get(position).getUserName());
            holder.profilePictureView.setProfileId(mList.get(position).getUserProfId());

            //Do something to distinguish a friend from not a friend
            if (Globals.userFriendList.contains(mList.get(position))) holder.cardView.setCardBackgroundColor(getResources().getColor(R.color.fab_staying_in_color));
            else holder.cardView.setCardBackgroundColor(getResources().getColor(R.color.cardColor));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mList!=null)return mList.size();
            else return 0;
        }
    }
}

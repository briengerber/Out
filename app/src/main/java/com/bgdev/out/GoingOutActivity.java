package com.bgdev.out;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bgdev.out.backend.userRecordApi.model.UserRecord;
import com.facebook.widget.ProfilePictureView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class GoingOutActivity extends ActionBarActivity implements View.OnClickListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    EditText whoIsGoingEditText,descriptionEditText;
    Button accept,cancel;
    WhoIsGoingAdapter adapter;
    ListPopupWindow listPopupWindow;
    static List<UserRecord> tempUserList = new ArrayList<UserRecord>();
    static List<Long> addToStatusLongList = new ArrayList<Long>();
    static List<UserRecord> previousUserList = new ArrayList<UserRecord>();

    //Places API Stuff
    AutoCompleteTextView mAutocompleteView;
    PlacesAutocompleteAdapter mAdapter;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LatLngBounds mBounds;
    String placeString;

    Context context;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_going_out);

        //Toolbars and context
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;

        //Configure variables
        tempUserList.clear();

        //TODO:find a way later to show the existing list of users on this status. For now clear every time
        addToStatusLongList.clear();

        //Places Stuff
        mAutocompleteView = (AutoCompleteTextView) findViewById(R.id.autocomplete_places);
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener); // Register a listener that receives callbacks when a suggestion has been selected
        buildGoogleApiClient();

        //Buttons
        accept = (Button) findViewById(R.id.acceptButton);
        cancel = (Button) findViewById(R.id.cancelButton);
        accept.setOnClickListener(this);
        cancel.setOnClickListener(this);

        //EditText for people searching
        whoIsGoingEditText = (EditText) findViewById(R.id.whoIsGoingEditText);
        setupWhoIsGoingEditText();

        //EditText for people the description
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);

    }

    public void setupWhoIsGoingEditText(){
        listPopupWindow = new ListPopupWindow(this);
        setupWhoIsGoingListPopup(listPopupWindow, Globals.userFriendList);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserRecord rec = tempUserList.get(position);
                if (!addToStatusLongList.contains(rec.getId())) {
                    addToStatusLongList.add(rec.getId());
                    Toast.makeText(context, rec.getUserName(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        whoIsGoingEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                for (int i=0;i < Globals.userFriendList.size(); i++) {
                    UserRecord userRecord = Globals.userFriendList.get(i);
                    String name = userRecord.getUserName();
                    boolean a = name.toLowerCase().contains(s.toString().toLowerCase());
                    boolean b = tempUserList.contains(userRecord);
                    if (a && !b) {
                        tempUserList.add(userRecord);
                    } else if (!a && b) {
                        tempUserList.remove(userRecord);
                    }
                }

                if (s.length() == 0) {
                    tempUserList.clear();
                }

                Collections.sort(tempUserList, new Comparator<UserRecord>() {
                    @Override
                    public int compare(UserRecord lhs, UserRecord rhs) {
                        return lhs.getUserName().compareTo(rhs.getUserName());
                    }
                });


                if (!tempUserList.isEmpty()) {
                    setupWhoIsGoingListPopup(listPopupWindow, tempUserList);
                    listPopupWindow.show();
                } else {
                    listPopupWindow.dismiss();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void setupWhoIsGoingListPopup(ListPopupWindow listPopupWindow, List<UserRecord> list){
        adapter = new WhoIsGoingAdapter(this,R.layout.rview_item_popupwindow,list);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setAnchorView(whoIsGoingEditText);
//        whoIsGoingEditText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.acceptButton:
                new MyAsyncTasks.CreateOrUpdateStatusAsyncTask(context,3,addToStatusLongList,placeString,descriptionEditText.getText().toString()).execute();
                setResult(RESULT_OK, new Intent(this, MainActivity.class));
                finish();
                break;
            case R.id.cancelButton:
                setResult(RESULT_CANCELED,new Intent(this,MainActivity.class));
                finish();
                break;
        }
    }

    private void buildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned and which Google APIs our app uses.
        if (mGoogleApiClient==null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, 0 /* clientId */, this)
                    .addConnectionCallbacks(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mBounds = new LatLngBounds.Builder().include(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())).build();
        mAdapter = new PlacesAutocompleteAdapter(context,R.layout.rview_item_places_adapter_item, mBounds, null);
        mAdapter.setGoogleApiClient(mGoogleApiClient);
        mAutocompleteView.setAdapter(mAdapter);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"Google Play Services Suspended",Toast.LENGTH_SHORT).show();
        mAdapter.setGoogleApiClient(null);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Errors
        Toast.makeText(this,"Google Play Services Connection Suspended",Toast.LENGTH_SHORT).show();
    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a PlaceAutocomplete object from which we
             read the place ID.
              */
            final PlacesAutocompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            placeString = place.getName().toString();
            places.release();
        }
    };


    public class WhoIsGoingAdapter extends ArrayAdapter<UserRecord> {
        List<UserRecord> mList;
        public WhoIsGoingAdapter(Context context, int layoutId,List<UserRecord> list){
            super(context,layoutId,list);
            mList = list;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position,View convertView,ViewGroup parent){
            View rowLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rview_item_popupwindow, null);

            TextView name = (TextView) rowLayoutView.findViewById(R.id.popup_user_name);
            ProfilePictureView pic = (ProfilePictureView) rowLayoutView.findViewById(R.id.popup_profile_picture);

            name.setText(mList.get(position).getUserName());
            pic.setProfileId(mList.get(position).getUserProfId());

            return rowLayoutView;
        }
    }
}

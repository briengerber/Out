package com.bgdev.out;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;


public class MainActivity extends AppCompatActivity{

    //Context
    static Context context;

    static FragmentManager fm;
    boolean noBack,bPaused,bIntentHit=false;
    String loadFragment = "";

    static AlarmManager alarmManager;

    //ActionBar
    DrawerLayout mDrawerLayout;
    NavigationView navigationView;
    static ListView mDrawerList;
    static Toolbar mToolbar;
    static ActionBar mActionBar;
    ActionBarDrawerToggle mDrawerToggle;
    //Cloud Stuff

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context=this;
        if (alarmManager==null) alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        //No back between main and login activity
        Bundle extras = getIntent().getExtras();
        if (extras!=null) noBack = getIntent().getExtras().getBoolean(Globals.DONT_BACK_KEY);

        //Shared Preferences
        if (Globals.settings==null) Globals.settings = getSharedPreferences(Globals.PREFERNCES, 0);
        Globals.editor = Globals.settings.edit();

        //Shared Preference from the setting menu
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (Globals.customSettings==null) Globals.customSettings = PreferenceManager.getDefaultSharedPreferences(this);
        Globals.customEditor = Globals.customSettings.edit();
        Globals.numberOfStatusToLoad = Integer.parseInt(Globals.customSettings.getString("home_record_limit", "25"));

        loadFragment = Globals.settings.getString(Globals.INTENT_HIT_KEY, "");
        loadAlarmSharedPreferences(alarmManager);

        fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() > 0) noBack = true;
                else noBack = false;

                int menuItem= -1;
                Fragment frag = fm.findFragmentById(R.id.content_frame);
                if (frag instanceof HomeFragment) menuItem=0;
                else if (frag instanceof SearchFragment) menuItem=1;
                else if (frag instanceof MessagesFragment) menuItem=2;
                else if (frag instanceof InvitesFragment) menuItem=3;

                if (navigationView!=null && menuItem!=-1) navigationView.getMenu().getItem(menuItem).setChecked(true);
            }
        });

        makeLoginRequest(Session.getActiveSession());
        setupNavigationAndToolbar();

        //TODO: Add some filtering based on when to load this fragment
        Fragment frag = null;
        if (!loadFragment.equals("")){
            if (loadFragment.equals("mess")) {
                frag = new MessagesFragment();
                Globals.editor.putString(Globals.INTENT_HIT_KEY, "").commit();
            }
        }
        else{
            frag = fm.findFragmentByTag("Frag");
            if (frag==null) frag = new HomeFragment();
        }

        if (frag!=null) fm.beginTransaction().replace(R.id.content_frame, frag, "Frag").commit();
    }

    public void makeLoginRequest(final Session session) {
        Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(final GraphUser user, Response response) {
                // If the response is successful
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        Globals.bProfileLoaded = true;

                        Globals.editor=Globals.settings.edit();
                        Globals.editor.putBoolean(Globals.LOGGED_IN_KEY, Globals.bProfileLoaded);
                        Globals.editor.commit();

                        Globals.USER_PROF_ID=user.getId();
                        new MyAsyncTasks.CreateUserAsyncTask(context,user.getId(),user.getName()).execute();
                    }
                }
                if (response.getError() != null) {
                    //ext,"Facebook Login Error",Toast.LENGTH_SHORT).show();
                }// Handle errors, will do so later.

            }
        });
        request.executeAsync();
    }

    public void setupNavigationAndToolbar(){
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        //Set up the header
        View header = LayoutInflater.from(this).inflate(R.layout.drawer_list_item_top,null);
        RelativeLayout drawerHeader = (RelativeLayout) header.findViewById(R.id.header_view_layout);

        //Profile Image
        ImageView circleImage = (ImageView) drawerHeader.findViewById(R.id.drawer_profile_picture);
        if (Globals.profPicBitMap==null) new MyAsyncTasks.UpdateProfPicBitMap(circleImage).execute();
        else circleImage.setImageBitmap(Globals.profPicBitMap);

        //Person Name
        TextView nameText = (TextView) drawerHeader.findViewById(R.id.drawer_name_textView);
        nameText.setText(Globals.USER_NAME);

        //Add the headerview
        navigationView.addHeaderView(header);
        drawerHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.closeDrawers();
                Fragment frag = ProfileFragment.newInstance(Globals.myUser, Globals.UNIQ_ID);
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.content_frame, frag);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                fm.executePendingTransactions();
            }
        });

        //Set on click for the items
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
//                if (menuItem.getItemId()!=R.id.nav_settings) {
//                    if (menuItem.isChecked()) {
//                        menuItem.setChecked(false);
//                    } else {
//                        menuItem.setChecked(true);
//                    }
//                }

                mDrawerLayout.closeDrawers();

                Fragment frag = null;
                switch (menuItem.getItemId()) {
                    case R.id.nav_home:
                        frag = new HomeFragment();
                        break;
                    case R.id.nav_search:
                        frag = new SearchFragment();
                        break;
                    case R.id.nav_messages:
                        frag = new MessagesFragment();
                        break;
                    case R.id.nav_invites:
                        frag = new InvitesFragment();
                        break;
                    case R.id.nav_settings:
                        if (Globals.bToolbarHidden) mToolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                        Intent prefs = new Intent(context, PrefsActivity.class);
                        startActivity(prefs);
                        return true;
                }

                if (frag != null) {
                    if (Globals.bToolbarHidden) mToolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator()).start();
                    FragmentTransaction fragmentTransaction = fm.beginTransaction();
                    fragmentTransaction.replace(R.id.content_frame, frag,"Frag");
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    fm.executePendingTransactions();
                    return true;
                }

                return false;
            }
        });

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,mToolbar,R.string.app_name,R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    public static Toolbar getToolbar(){return mToolbar;}

    private void loadAlarmSharedPreferences(AlarmManager manager){
        if (!Globals.settings.getBoolean("AlarmsSet",false)){
            Long update = Long.parseLong(Globals.customSettings.getString("server_update_frequency", "86400000"));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this,1,new Intent(Globals.SERVER_UPDATE_BROADCAST),PendingIntent.FLAG_UPDATE_CURRENT);
            manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+update,update,pendingIntent);
            Globals.editor.putBoolean("AlarmsSet", true).commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!noBack) {
            setResult(RESULT_OK);
            this.finish();
        }
        else {
            if (mDrawerLayout.isDrawerOpen(navigationView)) mDrawerLayout.closeDrawers();
            super.onBackPressed();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        bPaused=true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bPaused) makeLoginRequest(Session.getActiveSession());
        if (!Globals.bProfileLoaded){
            Intent login = new Intent(this,LoginActivity.class);
            startActivity(login);
        }

        if (Globals.settings==null) Globals.settings = getSharedPreferences(Globals.PREFERNCES, 0);
        if (Globals.customSettings==null) Globals.customSettings = PreferenceManager.getDefaultSharedPreferences(this);

        Globals.USER_PROF_ID = Globals.settings.getString(Globals.USER_PROF_ID_KEY,"");
        Globals.USER_NAME = Globals.settings.getString(Globals.USER_NAME_KEY,"");
        Globals.bProfileLoaded = Globals.settings.getBoolean(Globals.LOGGED_IN_KEY,false);
        Globals.UNIQ_ID = Globals.settings.getLong(Globals.PROPERTY_UNIQ_ID,0);
        Globals.DEVICE_REG_ID = Globals.settings.getString(Globals.PROPERTY_REG_ID,"");
    }
}

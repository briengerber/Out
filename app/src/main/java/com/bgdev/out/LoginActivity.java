package com.bgdev.out;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphLocation;
import com.facebook.widget.LoginButton;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    boolean isResumed = false;
    Session.StatusCallback callback;
    UiLifecycleHelper uiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Globals.settings = getSharedPreferences(Globals.PREFERNCES, 0);

        Globals.USER_PROF_ID = Globals.settings.getString(Globals.USER_PROF_ID_KEY, "");
        Globals.USER_NAME = Globals.settings.getString(Globals.USER_NAME_KEY, "");
        Globals.bProfileLoaded = Globals.settings.getBoolean(Globals.LOGGED_IN_KEY, false);
        Globals.UNIQ_ID = Globals.settings.getLong(Globals.PROPERTY_UNIQ_ID, 0);
        Globals.DEVICE_REG_ID = Globals.settings.getString(Globals.PROPERTY_REG_ID, "");

        LoginButton button = (LoginButton) findViewById(R.id.authButton);
        button.setReadPermissions(Arrays.asList("user_friends"));

        callback = new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                onSessionStateChange(state);
            }};

        uiHelper =  new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        Log.i("LoginActivity","onCreate");

    }

    private void onSessionStateChange(SessionState state){
        if (isResumed && !isFinishing()){
            if (state.isOpened()){
                Globals.bProfileLoaded = true;
                Intent i= new Intent(this,MainActivity.class);
                i.putExtra(Globals.DONT_BACK_KEY, true);
                startActivityForResult(i, 0);
                finish();
                Log.i(Globals.LOGIN_TAG, "Logged in...");
            }
            else if (state.isClosed()){
                Globals.bProfileLoaded = false;
                Log.i(Globals.LOGIN_TAG, "Logged out...");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed=false;
        uiHelper.onPause();
        Log.i("LoginActivity", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        isResumed=true;
        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null &&(session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session.getState());
        }
        Log.i("LoginActivity","onResume");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
        Log.i("LoginActivity", "onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHelper.onStop();
        Log.i("LoginActivity", "onStop");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        uiHelper.onActivityResult(requestCode, resultCode, data);
        Log.i("LoginActivity", "onActivityResult");
        //this.finish();
    }

}

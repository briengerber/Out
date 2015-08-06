package com.bgdev.out;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

public class PrefsActivity extends AppCompatActivity{

    static Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        context = this;

        if (Globals.listener==null) Globals.listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("server_update_frequency")) {
                    AlarmManager manager = MainActivity.alarmManager;
                    if (manager==null) manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Long update = Long.parseLong(sharedPreferences.getString("server_update_frequency", "86400000"));
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, new Intent(Globals.SERVER_UPDATE_BROADCAST), PendingIntent.FLAG_UPDATE_CURRENT);
                    manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + update, update, pendingIntent);
                }
                if (key.equals("home_record_limit")){
                    Globals.numberOfStatusToLoad = Integer.parseInt(sharedPreferences.getString("home_record_limit","25"));
                }
            }
        };
        getFragmentManager().beginTransaction().replace(R.id.content_frame,new PrefsFragment(),"Settings").commit();
    }

    @Override
    protected void onPause() {
        Globals.customSettings.unregisterOnSharedPreferenceChangeListener(Globals.listener);
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        Globals.customSettings.registerOnSharedPreferenceChangeListener(Globals.listener);
        super.onPostResume();
    }

}

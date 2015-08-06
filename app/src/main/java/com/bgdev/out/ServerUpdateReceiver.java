package com.bgdev.out;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServerUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Globals.settings==null) Globals.settings = context.getSharedPreferences(Globals.PREFERNCES, 0);
        Globals.UNIQ_ID = Globals.settings.getLong(Globals.PROPERTY_UNIQ_ID, 0);

        new MyAsyncTasks.GetUserFriendsAsyncTask().execute();
        new MyAsyncTasks.QueryStatusUpdateAsyncTask(false,0).execute();
        new MyAsyncTasks.QueryFriendInvitesAsyncTask().execute();
        new MyAsyncTasks.QueryAllUsersAsyncTask().execute();
        new MyAsyncTasks.QueryActiveConversationsAsyncTask().execute();
    }
}

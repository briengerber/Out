package com.bgdev.out;


import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;


import com.facebook.Session;

public class LogoutDialogPreference extends DialogPreference
{
    Context context;
    public LogoutDialogPreference(Context ctx,AttributeSet set){
        super(ctx,set);
        setTitle(Globals.USER_NAME);
        context = ctx;
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult){
            Session.getActiveSession().closeAndClearTokenInformation(); //This is how you log out. Set global variables and finish activity too.
            Globals.editor.clear();
            Globals.editor.commit();
            Globals.bProfileLoaded=false;
            ((Activity) context).finish();
        }
    }

}

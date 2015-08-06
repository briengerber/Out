package com.bgdev.out;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.provider.Settings;
import android.widget.GridLayout;

import com.bgdev.out.backend.conversationApi.model.Conversation;
import com.bgdev.out.backend.userRecordApi.model.UserRecord;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StaticConvertMethods {

    public static String convertIntStatusToString(int status){
        switch (status){
            case 1: return "Staying in";
            case 2: return "Undecided";
            case 3: return "Going Out";
            default:return "";
        }
    }
    public static String convertLongDiffToMessageStamp(long timeInMilli){
        long minutes,hours,days;
        Long diff = System.currentTimeMillis() - timeInMilli;

        minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        hours = TimeUnit.MILLISECONDS.toHours(diff);
        days = TimeUnit.MILLISECONDS.toDays(diff);

        Calendar prev,curr;
        int prevHour,prevDayOfWeek,prevDayofMonth,prevMonth,prevMin,prevAmPm;

        prev = Calendar.getInstance();
        prev.setTimeInMillis(timeInMilli);

        prevMin = prev.get(Calendar.MINUTE);
        prevAmPm = prev.get(Calendar.AM_PM);
        prevHour=prev.get(Calendar.HOUR);
        prevDayOfWeek = prev.get(Calendar.DAY_OF_WEEK);
        prevDayofMonth = prev.get(Calendar.DAY_OF_MONTH);
        prevMonth = prev.get(Calendar.MONTH);

        if (minutes<1 && hours < 1 && days <1) return   "now";
        else if (minutes >0 && hours <1 && days <1) return Long.toString(minutes) + "m";
        else if (hours >0 && days <1) return Long.toString(hours) + "h";
        else {
            String timeOfDay = getTimeOfDay(prevHour,prevMin,prevAmPm);
            if (days<7) {
                String dayOfWeek=getDayOfWeek(prevDayOfWeek);
                return dayOfWeek + " " + timeOfDay;
            }
            else{
                String dayOfMonth = getDayOfMonth(prevDayofMonth,prevMonth);
                return dayOfMonth + ", " + timeOfDay;
            }
        }
    }

    public static String convertLongDiffToTimeStamp(long timeInMilli){
        long minutes,hours,days;
        Long diff = System.currentTimeMillis() - timeInMilli;

        minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        hours = TimeUnit.MILLISECONDS.toHours(diff);
        days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes<1 && hours < 1 && days <1) return "<1m";
        else if (minutes >0 && hours <1 && days <1) return Long.toString(minutes) + "m";
        else if (hours >0 && days <1) return Long.toString(hours) + "h";
        else return Long.toString(days) + "d";
    }

    public static UserRecord findFriendUserRecordFromID(Long id){
        UserRecord rec=null;
        boolean bFound = false;

        List<UserRecord> tempList = new ArrayList<>();
        if (Globals.userFriendList!=null) {
            for (int i = 0; i <Globals.userFriendList.size();i++) {
                UserRecord record = Globals.userFriendList.get(i);
                tempList.add(record);
            }
            tempList.add(Globals.myUser);

            for (UserRecord friend : tempList) {
                if (friend.getId().equals(id)) {
                    rec = friend;
                    bFound = true;
                }
                if (bFound) break;
            }
        }
        return rec;
    }

    public static String getDayOfWeek(int day){
        String strDay="";
        switch (day){
            case 1: strDay = "Sun";
                break;
            case 2: strDay = "Mon";
                break;
            case 3: strDay = "Tues";
                break;
            case 4: strDay = "Wed";
                break;
            case 5: strDay = "Thu";
                break;
            case 6: strDay = "Fri";
                break;
            case 7: strDay = "Sat";
                break;
        }
        return strDay;
    }

    public static String getTimeOfDay(int hour,int minute,int AMPM){
        String strAM="";
        if (AMPM==0) strAM = "AM";
        else strAM = "PM";

        String strMin = "";
        if (minute<10) strMin = "0"+ minute;
        else strMin = Integer.toString(minute);

        return hour + ":" + strMin + " " + strAM;
    }

    public static String getDayOfMonth(int day, int month) {
        String strMonth="";
        switch (month){
            case 0: strMonth="Jan";
                break;
            case 1: strMonth="Feb";
                break;
            case 2: strMonth="Mar";
                break;
            case 3: strMonth="Apr";
                break;
            case 4: strMonth="May";
                break;
            case 5: strMonth="Jun";
                break;
            case 6: strMonth="Jul";
                break;
            case 7: strMonth="Aug";
                break;
            case 8: strMonth="Sep";
                break;
            case 9: strMonth="Oct";
                break;
            case 10: strMonth="Nov";
                break;
            case 11: strMonth="Dec";
                break;
        }

        return strMonth + " "+ day;
    }

    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        Bitmap output = null;
        if (bitmap!=null) {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawOval(rectF, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
        }
        return output;
    }

    public static Bitmap returnBigPicBitMap(String userId,int type){
        String strType="";
        if (type==1){
            strType="type=large";
        }
        else{
            strType="width=240&height=240";
        }
        Bitmap bitmap = null;
        try{
            URL imageURL = new URL("https://graph.facebook.com/" + userId + "/picture?"+strType);
            HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
            connection.setInstanceFollowRedirects(true);
            bitmap= BitmapFactory.decodeStream(connection.getInputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }

        if (type==1) return getRoundedBitmap(bitmap);
        else return bitmap;
    }
}



package ru.appkode.school.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ru.appkode.school.Infos;
import ru.appkode.school.data.ParcelableServerInfo;

/**
 * Created by lexer on 17.08.14.
 */
public class ServerSharedPreferences {

    private static final String TAG = "sharedPreferences";

    private Context mContext;

    public ServerSharedPreferences(Context сontext) {
        this.mContext = сontext;
    }

    public boolean checkSharedPreferences(ParcelableServerInfo serverInfo) {
        Log.d(TAG, "check shared preferences");
        SharedPreferences preferences = mContext.getSharedPreferences(Infos.PREFERENCES, Context.MODE_PRIVATE);
        String id = preferences.getString(Infos.ID, "unknown");

        Log.d(TAG, "id = " + id);
        if (id.substring(0, 4).equals("serv")) {
            String name = preferences.getString(Infos.SERVER_NAME, "unknown");
            String secondName = preferences.getString(Infos.SERVER_SECONDNAME, "unknown");
            String lastName = preferences.getString(Infos.SERVER_LASTNAME, "unknown");
            String subject = preferences.getString(Infos.SERVER_SUBJECT, "unknown");
            Log.d(TAG, "sp readed " + name + " " + secondName + " " + lastName + " " + subject);
            if (name.equals("unknown") || secondName.equals("unknown") || lastName.equals("unknown") || subject.equals("unknown")) {
                return false;
            } else {
                serverInfo.name = name;
                serverInfo.secondName = secondName;
                serverInfo.lastName = lastName;
                serverInfo.subject = subject;
                serverInfo.id = id;
                Log.d(TAG, "return true");
                return true;
            }
        }

        return false;
    }

    public void writeSharedPreferences(ParcelableServerInfo info) {
        Log.d(TAG, "write sp");

        SharedPreferences preferences = mContext.getSharedPreferences(Infos.PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(Infos.ID, info.id);
        editor.putString(Infos.SERVER_NAME, info.name);
        editor.putString(Infos.SERVER_SECONDNAME, info.secondName);
        editor.putString(Infos.SERVER_LASTNAME, info.lastName);
        editor.putString(Infos.SERVER_SUBJECT, info.subject);
        editor.commit();
    }

}

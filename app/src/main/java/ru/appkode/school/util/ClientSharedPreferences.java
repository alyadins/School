package ru.appkode.school.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ru.appkode.school.Infos;
import ru.appkode.school.data.ParcelableClientInfo;

/**
 * Created by lexer on 15.08.14.
 */
public class ClientSharedPreferences {
    private Context mContext;

    public ClientSharedPreferences(Context context) {
        mContext = context;
    }

    public boolean checkSharedPreferences(ParcelableClientInfo clientInfo) {
        SharedPreferences preferences = mContext.getApplicationContext().getSharedPreferences(Infos.PREFERENCES, Context.MODE_PRIVATE);
        String id = preferences.getString(Infos.ID, "nonesavedid");

        if (id.substring(0, 6).equals("client")) {
            String name = preferences.getString(Infos.CLIENT_NAME, "unknown");
            String lastName = preferences.getString(Infos.CLIENT_LASTNAME, "unknown");
            String group = preferences.getString(Infos.CLIENT_GROUP, "unknown");
            String blockBy = preferences.getString(Infos.CLIENT_BLOCKBY, "none");

            if (name.equals("unknown")  || lastName.equals("unknown") || group.equals("unknown")) {
                return false;
            } else {
                clientInfo.name = name;
                clientInfo.lastName = lastName;
                clientInfo.group = group;
                clientInfo.clientId = id;
                if (!blockBy.equals("none")) {
                    clientInfo.isBlocked = true;
                    clientInfo.blockedBy = blockBy;
                } else {
                    clientInfo.isBlocked = false;
                }
                return true;
            }
        }

        return false;
    }

    public void writeSharedPreferences(ParcelableClientInfo info) {

        SharedPreferences preferences = mContext.getApplicationContext().getSharedPreferences(Infos.PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(Infos.ID, info.clientId);
        editor.putString(Infos.CLIENT_NAME, info.name);
        editor.putString(Infos.CLIENT_LASTNAME, info.lastName);
        editor.putString(Infos.CLIENT_GROUP, info.group);
        editor.putString(Infos.CLIENT_BLOCKBY, info.blockedBy);

        editor.commit();
    }
}

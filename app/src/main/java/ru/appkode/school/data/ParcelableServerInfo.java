package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 01.08.14.
 */
public class ParcelableServerInfo implements Parcelable {
    public String subject;
    public String name;
    public String secondName;
    public String lastName;
    public String serverId;

    public boolean isFavourite = false;
    public boolean isConnected = false;
    public boolean isLocked = false;

    public ParcelableServerInfo() {
    }

    public ParcelableServerInfo(Parcel in) {
        String[] strings = new String[5];
        in.readStringArray(strings);
        this.subject = strings[0];
        this.name = strings[1];
        this.secondName = strings[2];
        this.lastName = strings[3];
        this.serverId = strings[4];

        boolean[] booleans = new boolean[3];
        in.readBooleanArray(booleans);
        this.isFavourite = booleans[0];
        this.isConnected = booleans[1];
        this.isLocked = booleans[2];
    }



    public ParcelableServerInfo(String lastName, String name, String secondName, String subject) {
        this.lastName = lastName;
        this.name = name;
        this.secondName = secondName;
        this.subject = subject;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.subject,
                this.name,
                this.secondName,
                this.lastName,
                this.serverId});
        dest.writeBooleanArray(new boolean[] {
                this.isFavourite,
                this.isConnected,
                this.isLocked});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ParcelableServerInfo createFromParcel(Parcel in) {
            return new ParcelableServerInfo(in);
        }

        public ParcelableServerInfo[] newArray(int size) {
            return new ParcelableServerInfo[size];
        }
    };

    public boolean isInit() {
        if (name != null && secondName != null && lastName != null && subject != null && serverId != null) {
            return true;
        } else {
            return false;
        }
    }
}

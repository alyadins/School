package ru.appkode.school.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lexer on 10.08.14.
 */
public class ParcelableClientInfo implements Parcelable {

    public String id;
    public String name;
    public String lastName;
    public String group;
    public String blockedBy = "none";

    public boolean isBlockedByOther = false;
    public boolean isBlocked = false;
    public boolean isChosen = false;

    public ParcelableClientInfo() {
    }

    public ParcelableClientInfo(String name, String lastName, String group) {
        this.name = name;
        this.lastName = lastName;
        this.group = group;
    }

    public ParcelableClientInfo(String name, String lastName, String group, String id) {
        this(name, lastName, group);
        this.id = id;
    }

    public ParcelableClientInfo(Parcel in) {
        String[] strings = new String[5];
        in.readStringArray(strings);
        this.id = strings[0];
        this.name = strings[1];
        this.lastName = strings[2];
        this.group = strings[3];
        this.blockedBy = strings[4];

        boolean[] booleans = new boolean[3];
        in.readBooleanArray(booleans);
        this.isBlockedByOther = booleans[0];
        this.isBlocked = booleans[1];
        this.isChosen = booleans[2];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.id,
                this.name,
                this.lastName,
                this.group,
                this.blockedBy});
        dest.writeBooleanArray(new boolean[] {
                this.isBlockedByOther,
                this.isBlocked,
                this.isChosen});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ParcelableClientInfo createFromParcel(Parcel in) {
            return new ParcelableClientInfo(in);
        }

        public ParcelableClientInfo[] newArray(int size) {
            return new ParcelableClientInfo[size];
        }
    };

    public boolean isInit() {
        if (name != null && lastName != null && group != null && blockedBy != null && id != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return name + " " + lastName + " " + group;
    }
}

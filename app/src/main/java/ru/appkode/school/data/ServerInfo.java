package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;

import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerInfo {
    public String name;
    public String subject;
    public boolean isFavourite;
    public boolean isConnected;

    public TeacherInfo teacherInfo;
    public NsdServiceInfo serviceInfo;

    public ServerInfo() {
    }

    public ServerInfo(String name, String subject, boolean isFavourite, boolean isConnected) {
        this.name = name;
        this.subject = subject;
        this.isFavourite = isFavourite;
        this.isConnected = isConnected;
    }
}

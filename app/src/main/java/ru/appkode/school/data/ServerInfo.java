package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;

import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerInfo {
    public String subject;
    public boolean isFavourite;
    public boolean isConnected;
    public String lastName;
    public String name;
    public String secondName;

    public String serverId;

    public NsdServiceInfo serviceInfo;

    public ServerInfo() {
    }

    public ServerInfo(String lastName, String name, String secondName, String subject) {
        this.lastName = lastName;
        this.name = name;
        this.secondName = secondName;
        this.subject = subject;
    }
}

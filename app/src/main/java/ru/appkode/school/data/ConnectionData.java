package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;

import java.net.InetAddress;

/**
 * Created by lexer on 11.08.14.
 */
public class ConnectionData {
    public String id;
    public InetAddress address;
    public String port;

    public ConnectionData(String id, InetAddress address, String port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }
}

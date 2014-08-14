package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;

import ru.appkode.school.network.Connection;

/**
 * Created by lexer on 11.08.14.
 */
public class ServerConnectionData {
    public String serverId;
    public NsdServiceInfo serviceInfo;
    public Connection connection;

    public ServerConnectionData(String serverId, NsdServiceInfo serviceInfo) {
        this.serverId = serverId;
        this.serviceInfo = serviceInfo;
    }
}

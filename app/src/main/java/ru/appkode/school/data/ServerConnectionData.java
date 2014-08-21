package ru.appkode.school.data;

import android.net.nsd.NsdServiceInfo;

/**
 * Created by lexer on 11.08.14.
 */
public class ServerConnectionData {
    public String serverId;
    public NsdServiceInfo serviceInfo;

    public ServerConnectionData(String serverId, NsdServiceInfo serviceInfo) {
        this.serverId = serverId;
        this.serviceInfo = serviceInfo;
    }
}

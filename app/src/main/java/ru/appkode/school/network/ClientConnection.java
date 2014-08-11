package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.data.ServerConnectionData;
import ru.appkode.school.util.JSONHelper;

import static ru.appkode.school.util.JSONHelper.*;


/**
 * Created by lexer on 04.08.14.
 */
public class ClientConnection implements Connection.OnMessageReceivedListener {


    public static final String SERVICE_TYPE = "_http._tcp.";

    private static final String TAG = "ClientConnection";

    private ArrayList<ParcelableServerInfo> mServersInfo;
    private ParcelableClientInfo mClientInfo;
    private List<ServerConnectionData> mServers;

    private static final int CAPACITY = 50;
    private static final int DELAY = 1500;

    public interface OnStatusChanged {
        public void OnStatusChanged(int status, String serverId);
    }

    public interface OnTeacherListChanged {
        public void onTeacherListChanged(ClientConnection connection, List<ParcelableServerInfo> serversInfo);
    }


    private Connection mConnection;

    private OnTeacherListChanged mOnTeacherListChanged;
    private OnStatusChanged mOnStatusChanged;


    public ClientConnection() {
        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mServers = new ArrayList<ServerConnectionData>();
    }

    public ClientConnection(ParcelableClientInfo clientInfo) {
        this();
        mClientInfo = clientInfo;
    }

    public void setClientInfo(ParcelableClientInfo clientInfo) {
        mClientInfo = clientInfo;
    }

    public void connect(ParcelableServerInfo serverInfo) {
        if (mClientInfo == null) {
            throw new IllegalArgumentException("client info is null, init it");
        }

        connectToServer(serverInfo, mClientInfo);
    }

    public void disconnect() {
        if (mClientInfo == null) {
            throw  new IllegalArgumentException("client info is null");
        }
        disconnectFromServer(mClientInfo);
    }

    public void askForServersInfo(List<NsdServiceInfo> infos) {
        ArrayList<ParcelableServerInfo> oldInfos = new ArrayList<ParcelableServerInfo>();
        oldInfos.addAll(mServersInfo);
        for (NsdServiceInfo info : infos) {
            if (!isServerInfoDownload(info, oldInfos))
                askForServerInfo(info);
        }
    }

    private boolean isServerInfoDownload(NsdServiceInfo info, ArrayList<ParcelableServerInfo> infos) {
        for (ParcelableServerInfo si : infos) {
            if (si.serverId.equals(info.getServiceName()))
                return true;
        }

        return false;
    }

    public ArrayList<ParcelableServerInfo> getServersInfo() {
        return mServersInfo;
    }

    private void askForServerInfo(final NsdServiceInfo serviceInfo) {
        try {
            Connection connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
            connection.setOnMessageReceivedListener(new Connection.OnMessageReceivedListener() {
                @Override
                public void onReceiveMessage(Connection connection, String message) {
                    Log.d("TEST", message);
                    try {
                        if (parseCode(message) == Server.INFO_CODE) {
                            ParcelableServerInfo serverInfo = parseServerInfo(message);
                            Log.d("TEST", "recived " + serverInfo.name + "  " + serverInfo.lastName);
                            mServers.add(new ServerConnectionData(serverInfo.serverId, serviceInfo));
                            mServersInfo.add(serverInfo);
                            connection.sendMessage("END");
                        } else {
                            Log.e(TAG, "server error");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            Log.d("TEST", "asked");
            connection.start();
            connection.sendMessage(createInfoRequest());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void connectToServer(ParcelableServerInfo info, ParcelableClientInfo clientInfo) {

        try {
            if (mConnection != null)
                mConnection.sendMessage(createClientDisconnect(clientInfo));
            NsdServiceInfo serviceInfo = getServerById(info.serverId).serviceInfo;
            mConnection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
            mConnection.start();
            mConnection.setOnMessageReceivedListener(this);
            mConnection.sendMessage(createClientConnect(clientInfo));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ServerConnectionData getServerById(String serverId) {
        for (ServerConnectionData data : mServers) {
            if (data.serverId.equals(serverId)) {
                return data;
            }
        }
        return null;
    }


    public void disconnectFromServer(ParcelableClientInfo clientInfo)  {
        if (mConnection != null) {
            try {
                mConnection.sendMessage(createClientDisconnect(clientInfo));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) throws JSONException {
        int code =  JSONHelper.parseCode(message);
        Log.d("TEST", message);
        //200
        if (mOnStatusChanged != null) {
            switch (code) {
                case Server.CONNECTED:
                    mOnStatusChanged.OnStatusChanged(code,JSONHelper.parseServerId(message));
                    break;
                case Server.DISCONNECTED:
                    mOnStatusChanged.OnStatusChanged(code, JSONHelper.parseServerId(message));
                    break;
            }
            if (code / 100 == 5 && mOnStatusChanged != null) {
                mOnStatusChanged.OnStatusChanged(code, JSONHelper.parseServerId(message));
            }
        }
        if (code / 100 == 4) {
            Log.e(TAG, "server error");
        }
    }

    public void setOnStatusChangedListener(OnStatusChanged l) {
        mOnStatusChanged = l;
    }

    public void setOnTeacherListChangedListener(OnTeacherListChanged l) {
        mOnTeacherListChanged = l;
    }
}

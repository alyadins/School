package ru.appkode.school.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.data.ClientInfo;
import ru.appkode.school.data.ServerInfo;


/**
 * Created by lexer on 04.08.14.
 */
public class ClientConnection implements NsdManager.ResolveListener, NsdManager.DiscoveryListener, Connection.OnMessageReceivedListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    private static final String TAG = "ClientConnection";

    private Context mContext;
    private NsdManager mNsdManager;
    private List<ServerInfo> mServersInfo;

    private String mServerId;

    private boolean mIsDiscoveryStarted = false;

    private Connection mConnection;

    private OnTeacherListChanged mOnTeacherListChanged;
    private OnStatusChanged mOnStatusChanged;

    public ClientConnection(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mServersInfo = new ArrayList<ServerInfo>();
    }

    public void discover() {
        Log.d("TEST", mIsDiscoveryStarted + " isStarted");
        if (!mIsDiscoveryStarted)
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
    }

    public void stopDiscover() {
        Log.d("TEST", mIsDiscoveryStarted + " isStarted");
        if (mIsDiscoveryStarted)
            mNsdManager.stopServiceDiscovery(this);
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        mIsDiscoveryStarted = false;
        Log.d("TEST", "discovery failed");
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        mIsDiscoveryStarted = false;
        Log.d("TEST", "stop dicovery fail");
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        mIsDiscoveryStarted = true;
        Log.d("TEST", "discovery started");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        mIsDiscoveryStarted = false;
        Log.d("TEST", "discovery stoped");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.d("TEST", "service found " + serviceInfo.getServiceName());
        mNsdManager.resolveService(serviceInfo, this);
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        removeService(serviceInfo);
    }


    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "resolve failed " + errorCode);
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Log.d("TEST", "on service resolved");
        addService(serviceInfo);
    }

    private void addService(NsdServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName();
        if (serviceName.substring(0, 4).equals("serv")
                && !isServiceAdded(serviceInfo)) {
            askForServerInfo(serviceInfo);
        } else {
        }
    }

    private void removeService(NsdServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName();
        if (serviceName.substring(0, 4).equals("serv")) {
            for (ServerInfo info : mServersInfo) {
                if (compareServicesInfo(serviceInfo, info.serviceInfo)) {
                    mServersInfo.remove(info);
                    if (mOnTeacherListChanged != null) {
                        mOnTeacherListChanged.onTeacherListChanged(this, mServersInfo);
                    }
                }
            }
        }
    }

    private boolean isServiceAdded(NsdServiceInfo si) {
        for (ServerInfo info : mServersInfo) {
            if (compareServicesInfo(si, info.serviceInfo)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareServicesInfo(NsdServiceInfo s1, NsdServiceInfo s2) {
        return s1.getServiceName().equals(s2.getServiceName());
    }

    private boolean compareServersInfo(ServerInfo s1, ServerInfo s2) {
        return compareServicesInfo(s1.serviceInfo, s2.serviceInfo);
    }

    private ServerInfo askForServerInfo(final NsdServiceInfo serviceInfo) {
        final ServerInfo info = new ServerInfo();
        try {
            Connection connection = new Connection(new Socket(serviceInfo.getHost(), serviceInfo.getPort()));
            connection.setOnMessageReceivedListener(new Connection.OnMessageReceivedListener() {
                @Override
                public void onReceiveMessage(Connection connection, String message) {
                    if (parseCode(message) == Server.INFO) {
                        addService(serviceInfo, parseTeacherInfoJson(message));
                        connection.sendMessage("END");
                    } else {
                        Log.e(TAG, "server error");
                    }
                }
            });
            connection.start();
            connection.sendMessage(getInfoJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return info;
    }

    private int parseCode(String message) {
        int code = 0;
        try {
            JSONObject json = new JSONObject(message);
            code = json.getInt("code");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return code;
    }

    private void addService(NsdServiceInfo serviceInfo, ServerInfo serverInfo) {
        serverInfo.serviceInfo = serviceInfo;
        mServersInfo.add(serverInfo);

        if (mOnTeacherListChanged != null) {
            mOnTeacherListChanged.onTeacherListChanged(this, mServersInfo);
        }
    }

    private ServerInfo parseTeacherInfoJson(String message) {
        try {
            JSONObject object = new JSONObject(message);
            String name = object.getString("name");
            String secondName = object.getString("second_name");
            String lastName = object.getString("last_name");
            String subject = object.getString("subject");
            String serverId = object.getString("server_id");
            ServerInfo info = new ServerInfo();
            info.serverId = serverId;
            info.subject = subject;
            info.lastName = lastName;
            info.secondName = secondName;
            info.name = name;
            return info;
        } catch (JSONException e) {
            e.printStackTrace();
        }



        return null;
    }

    public List<ServerInfo> getServersInfo() {
        return mServersInfo;
    }

    public void connectToServer(ServerInfo info, ClientInfo clientInfo) {
        NsdServiceInfo serviceInfo = info.serviceInfo;
        try {
            mConnection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
            mConnection.start();
            mConnection.setOnMessageReceivedListener(this);
            mConnection.sendMessage(getConnectJson(clientInfo));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectFromServer(ClientInfo clientInfo) {
        if (mConnection != null) {
            try {
                mConnection.start();
                mConnection.setOnMessageReceivedListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mConnection.sendMessage(getDisconnectJson(clientInfo));
        }
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) {
        int code =  parseCode(message);
        Log.d("TEST", message);
        //200
        if (mOnStatusChanged != null) {
            switch (code) {
                case Server.CONNECTED:
                    mServerId = getServerIdFromJson(message);
                    mOnStatusChanged.OnStatusChanged(code, mServerId);
                    break;
                case Server.DISCONNECTED:
                    mConnection.closeConnection();
                    break;
            }
            if (code / 100 == 5) {
                mOnStatusChanged.OnStatusChanged(code, mServerId);
            }
        }
        if (code / 100 == 4) {
            Log.e(TAG, "server error");
        }
    }

    private String getServerIdFromJson(String message) {
        String id = "";
        try {
            JSONObject json = new JSONObject(message);
            id = json.getString("server_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }


    private String getConnectJson(ClientInfo info) {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "connect");
            json.put("name", info.name);
            json.put("last_name", info.lastName);
            json.put("group", info.group);
            json.put("id", info.clientId);
            json.put("block", info.isBlocked);
            json.put("block_by", info.blockedBy);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    private String getDisconnectJson(ClientInfo info) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", info.clientId);
            jsonObject.put("method", "disconnect");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }


    private String getInfoJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "info");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    public void setOnstatusChangedListener(OnStatusChanged l) {
        mOnStatusChanged = l;
    }

    public interface OnStatusChanged {
        public void OnStatusChanged(int status, String serverId);
    }

    public void setOnTeacherListChangedListener(OnTeacherListChanged l) {
        mOnTeacherListChanged = l;
    }

    public interface OnTeacherListChanged {
        public void onTeacherListChanged(ClientConnection connection, List<ServerInfo> serversInfo);
    }
}

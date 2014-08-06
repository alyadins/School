package ru.appkode.school.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.data.ClientInfo;
import ru.appkode.school.data.ServerInfo;

/**
 * Created by lexer on 01.08.14.
 */
public class Server implements Connection.OnMessageReceivedListener, NsdManager.RegistrationListener, NsdManager.ResolveListener, NsdManager.DiscoveryListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final int INFO = 200;
    public static final int CONNECTED = 201;
    public static final int DISCONNECTED = 202;

    public static final int COMMAND_NOT_FOUND = 400;
    public static final int ALREADY_CONNECTED = 401;


    public static final int BLOCK_CODE = 500;
    public static final int UNBLOCK_CODE = 501;
    public static final int DELETE_CODE = 502;
    public static final int DISCONNECT = 503;


    private Context mContext;
    //Network
    private ServerSocket mServerSocket = null;
    private int mPort = -1;
    private NsdManager mNsdManager;

    //Data
    private String mServerName;
    private List<ClientInfo> mClientsInfo;
    private ServerInfo mServerInfo;

    //Listeners
    private OnClientListChanged mOnClientListChanged;
    private OnCheckName mOnCheckName;

    private boolean mIsRegistered;

    public Server(Context context, String serverName) {
        mServerName = serverName;
        mContext = context;
        mClientsInfo = new ArrayList<ClientInfo>();
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(0);
                mPort = mServerSocket.getLocalPort();
                registerService(mPort);

                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = mServerSocket.accept();
                    startConnection(socket);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void setServerInfo(ServerInfo info) {
        mServerInfo = info;
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) {
        processMessage(connection, message);
    }

    private void processMessage(Connection connection, String message) {

        String method = parseMethodFromJson(message);
        if (method.equals("info")) {
            connection.sendMessage(getTeacherInfoJson());
        } else if (method.equals("connect")) {
            ClientInfo info = parseUserInfoJson(message);
            if (isClientConnected(info)) {
                connection.sendMessage(createSimpleAnswerJson(ALREADY_CONNECTED, "already connected"));
            } else {
                info.connection = connection;
                mClientsInfo.add(info);
                if (mOnClientListChanged != null) {
                    mOnClientListChanged.onClientListChanged(mClientsInfo);
                }
                connection.sendMessage(createConnectionMessage(CONNECTED, "connected"));
            }
        } else if (method.equals("disconnect")) {
            String clientId = parseClientIdFromJson(message);
            if (removeClient(clientId)) {
                connection.sendMessage(createConnectionMessage(DISCONNECTED, "disconnected"));
                connection.closeConnection();
                removeClient(parseClientIdFromJson(message));
            }
        } else {
            connection.sendMessage(createSimpleAnswerJson(COMMAND_NOT_FOUND, "command not found"));
        }
    }

    private boolean isClientConnected(ClientInfo info) {
        for (ClientInfo i : mClientsInfo) {
            if (info.clientId.equals(i.clientId))
                return true;
        }

        return false;
    }

    private void startConnection(Socket socket) {
        Connection connection = new Connection(socket);
        connection.setOnMessageReceivedListener(this);
        try {
            connection.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectAllClients() {
        disconnect(mClientsInfo);
    }
    public void disconnect(List<ClientInfo> infoList) {
        for (ClientInfo info : infoList) {
            info.connection.sendMessage(getDisconnectJson());
            mClientsInfo.remove(info);
            info.connection.closeConnection();
        }
        if (mOnClientListChanged != null)
            mOnClientListChanged.onClientListChanged(mClientsInfo);
    }

    private boolean removeClient(String clientId) {
        for (ClientInfo info : mClientsInfo) {
            if (info.clientId.equals(clientId)) {
                mClientsInfo.remove(info);
                if(mOnClientListChanged != null) {
                    mOnClientListChanged.onClientListChanged(mClientsInfo);
                }
                return true;
            }
        }
        return false;
    }

    /*
        Json
     */

    private String createConnectionMessage(int code, String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("code", code);
            json.put("message", message);
            json.put("server_id", mServerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }


    private ClientInfo parseUserInfoJson(String message) {
        Log.d("TEST", message);
        ClientInfo info = new ClientInfo();
        try {
            JSONObject json = new JSONObject(message);
            info.name = json.getString("name");
            info.lastName = json.getString("last_name");
            info.group = json.getString("group");
            info.clientId = json.getString("id");
            info.isBlocked = json.getBoolean("block");
            info.blockedBy = json.getString("block_by");
            if (info.blockedBy.equals("none")) {
                Log.d("TEST", "eq none");
                info.isBlockedByOther = false;
            } else if (info.blockedBy.equals(mServerName)) {
                info.isBlockedByOther = true;
            } else {
                info.isBlockedByOther = false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }


    private String parseClientIdFromJson(String message) {
        String clientId = "";
        try {
            JSONObject json = new JSONObject(message);
            clientId = json.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return clientId;
    }

    private String parseMethodFromJson(String message) {
        String method = "";
        try {
            JSONObject json = new JSONObject(message);
            method = json.getString("method");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return method;
    }

    private String getTeacherInfoJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("code", INFO);
            json.put("name", mServerInfo.name);
            json.put("second_name", mServerInfo.secondName);
            json.put("last_name", mServerInfo.lastName);
            json.put("subject", mServerInfo.subject);
            json.put("server_id", mServerInfo.serverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    private String createSimpleAnswerJson(int code, String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("code", code);
            json.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }


    private void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServerName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, this);

    }

    private String getDisconnectJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("server_id", mServerName);
            json.put("code", DISCONNECT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    /*
        Start stop
     */

    public void start() {
        Thread thread = new Thread(new ServerThread());
        thread.start();
    }

    public void stop() {
        disconnectAllClients();
        mNsdManager.unregisterService(this);
    }

    /*
        Block unblock
     */
    public void block(List<ClientInfo> selectedClients) {
        for (ClientInfo info : selectedClients) {
            info.connection.sendMessage(createSimpleAnswerJson(BLOCK_CODE, "block"));
        }
    }

    public void unBlock(List<ClientInfo> selectedClients) {
        for (ClientInfo info : selectedClients) {
            info.connection.sendMessage(createSimpleAnswerJson(UNBLOCK_CODE, "unblock"));
        }
    }

    /*
        Interfaces
     */

    public void setOnNewUserConnectedListener(OnClientListChanged l) {
        mOnClientListChanged = l;
    }

    public interface OnClientListChanged {
        public void onClientListChanged(List<ClientInfo> clientsInfo);
    }

    public void setOnCheckNameListener(OnCheckName l) {
        mOnCheckName = l;
    }

    public interface OnCheckName {
        public void onCheckName(boolean free);
    }

    /*
        Registration
      */

    public void registerService() {
        if (mPort > -1 && !mIsRegistered && !mServerSocket.isClosed()) {
            registerService(mPort);
        }
    }

    public void unregisterService() {
        if (mIsRegistered)
            mNsdManager.unregisterService(this);
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "registration failed");
        mIsRegistered = false;
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "unregistaion failed");
        mIsRegistered = false;
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        Log.d("TEST", "registration successful");
        mIsRegistered = true;
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        Log.d("TEST", "unregistration successful");
        mIsRegistered = false;
    }

    /*
        Discovery
     */

    public boolean isNameFree(String name) {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
        return false;
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onDiscoveryStarted(String serviceType) {

    }

    @Override
    public void onDiscoveryStopped(String serviceType) {

    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {

    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {

    }

    /*
        Resolve
     */

    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {

    }


}

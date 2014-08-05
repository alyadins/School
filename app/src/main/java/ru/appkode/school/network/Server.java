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
import ru.appkode.school.data.TeacherInfo;

/**
 * Created by lexer on 01.08.14.
 */
public class Server implements Connection.OnMessageReceivedListener, NsdManager.RegistrationListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final int INFO = 200;
    public static final int CONNECTED = 201;
    public static final int DISCONNECTED = 202;

    public static final int COMMAND_NOT_FOUND = 400;
    public static final int ALREADY_CONNECTED = 401;


    public static final int BLOCK_CODE = 500;
    public static final int UNBLOCK_CODE = 501;
    public static final int DELETE_CODE = 502;

    private Context mContext;
    private ServerSocket mServerSocket = null;
    private int mPort = -1;

    private String mServerName;

    private List<Connection> mConnections;
    private List<Connection> mClientConnections;

    private List<ClientInfo> mClientsInfo;

    private TeacherInfo mTeacherInfo;
    private OnClientListChanged mOnClientListChanged;

    private NsdManager mNsdManager;
    private boolean mIsRegistered;

    public Server(Context context, String serverName) {
        mServerName = serverName;
        mContext = context;
        mClientsInfo = new ArrayList<ClientInfo>();
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mConnections = new ArrayList<Connection>();
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(0);
                mPort = mServerSocket.getLocalPort();
                registerService(mPort);

                Log.d("TEST", "port = " + mPort + "");
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

    private void startConnection(Socket socket) {
        Connection connection = new Connection(socket);
        mConnections.add(connection);
        connection.setOnMessageReceivedListener(this);
        try {
            connection.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTeacherInfo(TeacherInfo info) {
        mTeacherInfo = info;
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) {
        processMessage(connection, message);
    }

    private void processMessage(Connection connection, String message) {


        Log.d("TEST", message);
        String method = parseMethod(message);
        if (method.equals("info")) {
            connection.sendMessage(getTeacherInfoJson());
        } else if (method.equals("connect")) {
            ClientInfo info = parseUserInfoJson(message);
            if (isClientConnected(info)) {
                connection.sendMessage(createSimpleAnswerJson(ALREADY_CONNECTED, "already connected"));
            } else {
                info.connection = connection;
                mClientsInfo.add(info);
                mConnections.remove(connection);
                if (mOnClientListChanged != null) {
                    mOnClientListChanged.onClientListChanged(mClientsInfo);
                }
                connection.sendMessage(createSimpleAnswerJson(CONNECTED, "connected"));
            }
        } else if (method.equals("disconnect")) {
            String clientId = parseClientId(message);
            if (removeClient(clientId)) {
                connection.sendMessage(createSimpleAnswerJson(DISCONNECTED, "disconnected"));
            }
        } else {
            connection.sendMessage(createSimpleAnswerJson(COMMAND_NOT_FOUND, "command not found"));
        }
    }



    private ClientInfo parseUserInfoJson(String message) {
        ClientInfo info = new ClientInfo();
        try {
            JSONObject json = new JSONObject(message);
            info.name = json.getString("name");
            info.lastName = json.getString("last_name");
            info.group = json.getString("group");
            info.clientId = json.getString("id");
            info.isBlocked = json.getBoolean("block");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return info;
    }

    private boolean isClientConnected(ClientInfo info) {
        for (ClientInfo i : mClientsInfo) {
            if (info.clientId.equals(i.clientId))
                return true;
        }

        return false;
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

    private String parseClientId(String message) {
        String clientId = "";
        try {
            JSONObject json = new JSONObject(message);
            clientId = json.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return clientId;
    }

    private String parseMethod(String message) {
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
            json.put("name", mTeacherInfo.name);
            json.put("second_name", mTeacherInfo.secondName);
            json.put("last_name", mTeacherInfo.lastName);
            json.put("subject", mTeacherInfo.subject);
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

    public int getPort() {
        return mPort;
    }

    public void start() {
        Thread thread = new Thread(new ServerThread());
        thread.start();
    }

    private void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServerName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, this);

    }

    public void stop() {

        mNsdManager.unregisterService(this);
    }

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

    public void registerService() {
        if (mPort > -1 && !mIsRegistered && !mServerSocket.isClosed()) {
            registerService(mPort);
        }
    }

    public void unregisterService() {
        if (mIsRegistered)
            mNsdManager.unregisterService(this);
    }

    public void setOnNewUserConnectedListener(OnClientListChanged l) {
        mOnClientListChanged = l;
    }

    public interface OnClientListChanged {
        public void onClientListChanged(List<ClientInfo> clientsInfo);
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

}

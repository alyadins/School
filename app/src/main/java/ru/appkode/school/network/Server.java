package ru.appkode.school.network;

import android.os.Handler;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.appkode.school.data.ClientConnectionData;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.util.JSONHelper;

/**
 * Created by lexer on 01.08.14.
 */
public class Server implements Connection.OnMessageReceivedListener{

    public static final int INFO_CODE = 200;
    public static final int CONNECTED = 201;
    public static final int DISCONNECTED = 202;

    public static final int COMMAND_NOT_FOUND = 400;
    public static final int ALREADY_CONNECTED = 401;


    public static final int BLOCK_CODE = 500;
    public static final int UNBLOCK_CODE = 501;
    public static final int DISCONNECT = 502;

    public static final int INFO = 0;
    public static final int CONNECT = 1;

    /*
        Interfaces
     */

    public void setOnClientListChangedListener(OnClientListChanged l) {
        mOnClientListChanged = l;
    }

    public interface OnClientListChanged {
        public void onClientListChanged(ArrayList<ParcelableClientInfo> clientsInfo);
    }

    //Network
    private ServerSocket mServerSocket = null;
    private int mPort = -1;

    //Data
    private String mServerId;
    private ArrayList<ParcelableClientInfo> mClientsInfo;
    private List<ClientConnectionData> mClientConnections;
    private ParcelableServerInfo mServerInfo;

    //Listeners
    private OnClientListChanged mOnClientListChanged;

    private Thread mServerThread;

    public Server() {
        mClientsInfo = new ArrayList<ParcelableClientInfo>();
        mClientConnections = new ArrayList<ClientConnectionData>();
    }

    public void setServerInfo(ParcelableServerInfo info) {
        mServerInfo = info;
        mServerId = info.serverId;
    }

    public int getPort() {
        return mPort;
    }

    public String getId() {
        return mServerId;
    }

    /*
       Block unblock
    */
    public void block(List<ParcelableClientInfo> selectedClients, List<String> whiteList, List<String> blackList){
        for (ParcelableClientInfo info : selectedClients) {
            ClientConnectionData data = getConnectionDataById(info.clientId);
            ParcelableClientInfo infoForChange = getClientsInfoById(info.clientId);
            try {
                data.connection.sendMessage(JSONHelper.createBlockJson(mServerId, whiteList, blackList));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            infoForChange.isBlocked = true;
            infoForChange.isChosen = true;
            infoForChange.blockedBy = mServerId;
            infoForChange.isBlockedByOther = false;
        }
        if (mOnClientListChanged != null) {
            mOnClientListChanged.onClientListChanged((mClientsInfo));
        }
    }

    public void unBlock(List<ParcelableClientInfo> selectedClients) {
        for (ParcelableClientInfo info : selectedClients) {
            ClientConnectionData data = getConnectionDataById(info.clientId);
            ParcelableClientInfo parcelableClientInfo = getClientsInfoById(info.clientId);
            parcelableClientInfo.isBlockedByOther = false;
            parcelableClientInfo.isChosen = true;
            parcelableClientInfo.isBlocked = false;
            try {
                data.connection.sendMessage(JSONHelper.createConnectionMessage(UNBLOCK_CODE, "unblock", mServerId));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (mOnClientListChanged != null) {
            mOnClientListChanged.onClientListChanged(mClientsInfo);
        }
    }

    public void unBlockAll() {
        unBlock(mClientsInfo);
    }
    public ArrayList<ParcelableClientInfo> getClientsInfo() {
        return mClientsInfo;
    }

    public void disconnectAllClients() {
        disconnect(mClientsInfo);
    }

    public void disconnect(List<ParcelableClientInfo> infoList) {
        for (ParcelableClientInfo info : infoList) {
            final ClientConnectionData data = getConnectionDataById(info.clientId);
            ParcelableClientInfo infoForDelete = getClientsInfoById(info.clientId);
            try {
                data.connection.sendMessage(JSONHelper.createConnectionMessage(UNBLOCK_CODE, "block", mServerId));
                data.connection.sendMessage(JSONHelper.createServerDisconnect(mServerId));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mClientsInfo.remove(infoForDelete);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    data.connection.closeConnection();
                }
            }, 2000);
            mClientConnections.remove(data);
        }
        if (mOnClientListChanged != null)
            mOnClientListChanged.onClientListChanged(mClientsInfo);
    }

    public ParcelableClientInfo getClientsInfoById(String clientId) {
        for (ParcelableClientInfo info : mClientsInfo) {
            if (info.clientId.equals(clientId)) {
                return info;
            }
        }

        return null;
    }

     /*
        Start stop
     */

    public void start() {
        mServerThread = new Thread(new ServerThread());
        mServerThread.start();
        Log.d("TEST", "server started");
    }

    public void stop() {
        disconnectAllClients();
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) {
        try {
            processMessage(connection, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Connection connection, String message) throws JSONException {
        Log.d("TEST", message);
        int method = JSONHelper.parseMethod(message);
        switch (method) {
            case INFO:
                processInfo(connection);
                break;
            case CONNECT:
                processConnect(connection, message);
                break;
            case DISCONNECT:
                processDisconnect(connection, message);
                break;
            default:
                connection.sendMessage(JSONHelper.createSimpleAnswer(COMMAND_NOT_FOUND, "command not found"));
        }
    }

    private void processInfo(Connection connection) throws JSONException {
        connection.sendMessage(JSONHelper.createServerInfo(mServerInfo));
    }

    private void processConnect(Connection connection, String message) throws JSONException {
        ParcelableClientInfo info = JSONHelper.parseUserInfo(message, mServerId);

        if (isClientConnected(info.clientId)) {
            connection.sendMessage(JSONHelper.createSimpleAnswer(ALREADY_CONNECTED, "already connected"));
        } else {
            ClientConnectionData data = new ClientConnectionData();
            data.clientId = info.clientId;
            data.connection = connection;
            mClientsInfo.add(info);
            mClientConnections.add(data);
            if (mOnClientListChanged != null) {
                mOnClientListChanged.onClientListChanged(mClientsInfo);
            }
            connection.sendMessage(JSONHelper.createConnectionMessage(CONNECTED, "connected", mServerId));
        }
    }

    private void processDisconnect(Connection connection, String message) throws JSONException {
        String clientId = JSONHelper.parseClientId(message);
        if (removeClient(clientId)) {
            connection.sendMessage(JSONHelper.createConnectionMessage(DISCONNECTED, "disconnected", mServerId));
            connection.closeConnection();
            removeClient(JSONHelper.parseClientId(message));
        }
    }

    private boolean isClientConnected(String clientId) {
        for (ClientConnectionData connection : mClientConnections) {
            if (connection.clientId.equals(clientId))
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



    private ClientConnectionData getConnectionDataById(String clientId) {
        for (ClientConnectionData data : mClientConnections) {
            if (data.clientId.equals(clientId))
                return data;
        }
        return null;
    }

    private boolean removeClient(String clientId) {
        for (ParcelableClientInfo info : mClientsInfo) {
            if (info.clientId.equals(clientId)) {
                mClientsInfo.remove(info);
                mClientConnections.remove(getConnectionDataById(info.clientId));
                if(mOnClientListChanged != null) {
                    mOnClientListChanged.onClientListChanged(mClientsInfo);
                }
                return true;
            }
        }
        return false;
    }

    public ArrayList<Connection> getAllBlockedConnections() {
        ArrayList<Connection> connections = new ArrayList<Connection>();
        for (ParcelableClientInfo info : mClientsInfo) {
            if (info.isBlocked) {
                Connection connection = getConnectionDataById(info.clientId).connection;
                connections.add(connection);
            }
        }

        return connections;
    }


    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(0);
                mPort = mServerSocket.getLocalPort();
                Log.d("TEST", "Server socket has opened");
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
}

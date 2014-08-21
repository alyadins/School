package ru.appkode.school.network;

import android.util.Log;

import org.json.JSONException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.appkode.school.data.ConnectionData;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;

import static ru.appkode.school.network.ConnectionParams.*;
import static ru.appkode.school.util.JSONHelper.*;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerConnection implements MessageReceiver.OnMessageReceive {

    private static final String TAG = "ServerConnection";


    public interface OnClientListChanged {
        public void onClientsListChanged(ArrayList<ParcelableClientInfo> clientsInfo);
    }

    private ArrayList<ParcelableClientInfo> mClientsInfo;
    private List<ConnectionData> mClientsConnectionData;
    private ParcelableServerInfo mServerInfo;

    private MessageSender mSender;
    private MessageReceiver mReceiver;

    private ArrayList<String> mWhiteList;
    private ArrayList<String> mBlackList;

    private OnClientListChanged mOnClientListChanged;

    public ServerConnection() {
        mClientsInfo = new ArrayList<ParcelableClientInfo>();
        mClientsConnectionData = new ArrayList<ConnectionData>();

        mSender = new MessageSender();
        mReceiver = new MessageReceiver();
        while (!mReceiver.isInit()){};
        mReceiver.setOnMessageReceiveListener(this);
    }

    public String getPort() {
        return String.valueOf(mReceiver.getPort());
    }


    public ArrayList<ParcelableClientInfo> getClientsInfo() {
        return mClientsInfo;
    }

    public void setServerInfo(ParcelableServerInfo info) {
        mServerInfo = info;
    }

    public void setOnClientListChangedListener(OnClientListChanged l) {
        mOnClientListChanged = l;
    }

    public void blockClients(List<ParcelableClientInfo> clientsInfo) {
        for (ParcelableClientInfo info : clientsInfo) {
            block(info);
        }
    }

    public void unblockClients(List<ParcelableClientInfo> clientsInfo) {
        for (ParcelableClientInfo info : clientsInfo) {
            unblock(info);
        }
    }

    public void disconnectClients(List<ParcelableClientInfo> clientsInfo) {
        for (ParcelableClientInfo info : clientsInfo) {
            unblock(info);
            disconnect(info);
        }
    }

    public void setBlackList(ArrayList<String> blackList) {
        mBlackList = blackList;
    }


    public void setWhiteList(ArrayList<String> whiteList) {
        mWhiteList = whiteList;
    }

    @Override
    public void onMessageReceive(String message, InetAddress address) {
        Log.d(TAG, "receive message " + message + " from " + address.getHostName());
        try {
            String method = parseMethod(message);
            if (method.equals(INFO)) {
                methodInfo(message, address);
            } else if (method.equals(CONNECT)) {
                methodConnect(message, address);
            } else if (method.equals(DISCONNECT)) {
                methodDisconnect(message, address);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing method " + e.getMessage());
        }
    }

    private void methodInfo(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String port = params.get(PORT);

            HashMap<String, String> paramsForSend = getBaseParams();
            paramsForSend.put(NAME, mServerInfo.name);
            paramsForSend.put(SECOND_NAME, mServerInfo.secondName);
            paramsForSend.put(LAST_NAME, mServerInfo.lastName);
            paramsForSend.put(SUBJECT, mServerInfo.subject);

            String messageForSend = createMessage(INFO_CALLBACK, paramsForSend);

            mSender.sendMessage(messageForSend, address, port);
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params info" + e.getMessage());
        }
    }

    private void methodConnect(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            String name = params.get(NAME);
            String lastName = params.get(LAST_NAME);
            String group = params.get(GROUP);
            String blockBy = params.get(BLOCK_BY);
            String port = params.get(PORT);

            ParcelableClientInfo info = new ParcelableClientInfo(name, lastName, group, id);
            if (blockBy.equals("none")) {
                info.isBlocked = false;
                info.blockedBy = "none";
            } else {
                info.isBlocked = true;
                info.blockedBy = blockBy;
            }

            mClientsInfo.add(info);
            mClientsConnectionData.add(new ConnectionData(id, address, port));

            if (mOnClientListChanged != null) {
                mOnClientListChanged.onClientsListChanged(mClientsInfo);
            }

            HashMap<String, String> callBackParams = getBaseParams();

            String callBackMessage = createMessage(CONNECT_CALLBACK, callBackParams);
            mSender.sendMessage(callBackMessage, address, port);
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params connect " + e.getMessage());
        }
    }

    private void methodDisconnect(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            String port = params.get(PORT);

            ParcelableClientInfo info = getClientInfoById(id);
            ConnectionData connectionData = getClientConnectionDataById(id);

            if (info != null && connectionData != null) {
                mClientsInfo.remove(info);
                mClientsConnectionData.remove(connectionData);
            } else
                return;

            if (mOnClientListChanged != null) {
                mOnClientListChanged.onClientsListChanged(mClientsInfo);
            }

            HashMap<String, String> callBackParams = getBaseParams();

            String callBackMessage = createMessage(DISCONNECT_CALLBACK, callBackParams);
            mSender.sendMessage(callBackMessage, address, port);
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params disconnect " + e.getMessage());
        }
    }

    private void block(ParcelableClientInfo info) {
        if (mWhiteList == null || mBlackList == null) {
            throw new NullPointerException("init white list and black list");
        }
        try {
            ConnectionData data = getClientConnectionDataById(info.id);
            if (data != null) {
                String message = createBlockJson(mServerInfo.id, getPort(), mWhiteList, mBlackList);
                mSender.sendMessage(message, data.address, data.port);

                ParcelableClientInfo infoForChange = getClientInfoById(info.id);
                if (infoForChange != null) {
                    infoForChange.isBlocked = true;
                    infoForChange.blockedBy = mServerInfo.id;
                    infoForChange.isChosen = true;
                    if (mOnClientListChanged != null) {
                        mOnClientListChanged.onClientsListChanged(mClientsInfo);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with creating block message " + e.getMessage());
        }
    }

    private void unblock(ParcelableClientInfo info) {
        ConnectionData data = getClientConnectionDataById(info.id);
        try {
            HashMap<String, String> params = getBaseParams();
            if (data != null) {
                String message = createMessage(UNBLOCK, params);
                mSender.sendMessage(message, data.address, data.port);

                ParcelableClientInfo infoForChange = getClientInfoById(info.id);
                if (infoForChange != null) {
                    infoForChange.isBlocked = false;
                    infoForChange.blockedBy = "none";
                    infoForChange.isChosen = true;
                    if (mOnClientListChanged != null) {
                        mOnClientListChanged.onClientsListChanged(mClientsInfo);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with creating unblock message " + e.getMessage());
        }
    }


    private void disconnect(ParcelableClientInfo info) {
        ConnectionData data = getClientConnectionDataById(info.id);
        try {
            HashMap<String, String> params = getBaseParams();
            if (data != null) {
                String message = createMessage(DISCONNECT_FROM, params);
                mSender.sendMessage(message, data.address, data.port);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with creating disconnect message");
        }
    }

    private ParcelableClientInfo getClientInfoById(String id) {
        for (ParcelableClientInfo info : mClientsInfo) {
            if (info.id.equals(id)) {
                return info;
            }
        }

        return null;
    }

    private ConnectionData getClientConnectionDataById(String id) {
        for (ConnectionData data : mClientsConnectionData) {
            if (data.id.equals(id))
                return data;
        }
        return null;
    }

    private HashMap<String, String> getBaseParams() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(ID, mServerInfo.id);
        params.put(PORT, getPort());

        return params;
    }


//    /*
//       Block unblock
//    */
//    public void block(List<ParcelableClientInfo> selectedClients, List<String> whiteList, List<String> blackList){
//        for (ParcelableClientInfo info : selectedClients) {
//            ClientConnectionData data = getConnectionDataById(info.id);
//            ParcelableClientInfo infoForChange = getClientsInfoById(info.id);
//            try {
//                data.connection.sendMessage(JSONHelper.createBlockJson(mServerId, whiteList, blackList));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            infoForChange.isBlocked = true;
//            infoForChange.isChosen = true;
//            infoForChange.blockedBy = mServerId;
//            infoForChange.isBlockedByOther = false;
//        }
//        if (mOnClientListChanged != null) {
//            mOnClientListChanged.onClientListChanged((mClientsInfo));
//        }
//    }
//
//    public void unBlock(List<ParcelableClientInfo> selectedClients) {
//        for (ParcelableClientInfo info : selectedClients) {
//            ClientConnectionData data = getConnectionDataById(info.id);
//            ParcelableClientInfo parcelableClientInfo = getClientsInfoById(info.id);
//            parcelableClientInfo.isBlockedByOther = false;
//            parcelableClientInfo.isChosen = true;
//            parcelableClientInfo.isBlocked = false;
//            try {
//                data.connection.sendMessage(JSONHelper.createConnectionMessage(UNBLOCK_CODE, "unblock", mServerId));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        if (mOnClientListChanged != null) {
//            mOnClientListChanged.onClientListChanged(mClientsInfo);
//        }
//    }
//
//    public void unBlockAll() {
//        unBlock(mClientsInfo);
//    }
//    public ArrayList<ParcelableClientInfo> getClientsInfo() {
//        return mClientsInfo;
//    }
//
//    public void disconnectAllClients() {
//        disconnect(mClientsInfo);
//    }
//
//    public void disconnect(List<ParcelableClientInfo> infoList) {
//        for (ParcelableClientInfo info : infoList) {
//            final ClientConnectionData data = getConnectionDataById(info.id);
//            ParcelableClientInfo infoForDelete = getClientsInfoById(info.id);
//            try {
//                data.connection.sendMessage(JSONHelper.createConnectionMessage(UNBLOCK_CODE, "block", mServerId));
//                data.connection.sendMessage(JSONHelper.createServerDisconnect(mServerId));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            mClientsInfo.remove(infoForDelete);
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    data.connection.closeConnection();
//                }
//            }, 2000);
//            mClientsConnectionData.remove(data);
//        }
//        if (mOnClientListChanged != null)
//            mOnClientListChanged.onClientListChanged(mClientsInfo);
//    }
//
//    public ParcelableClientInfo getClientsInfoById(String clientId) {
//        for (ParcelableClientInfo info : mClientsInfo) {
//            if (info.id.equals(clientId)) {
//                return info;
//            }
//        }
//
//        return null;
//    }
//
//     /*
//        Start stop
//     */
//
//    public void start() {
//        mServerThread = new Thread(new ServerThread());
//        mServerThread.start();
//        Log.d("TEST", "server started");
//    }
//
//    public void stop() {
//        disconnectAllClients();
//    }
//
//    @Override
//    public void onReceiveMessage(Connection connection, String message) {
//        try {
//            processMessage(connection, message);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void processMessage(Connection connection, String message) throws JSONException {
//        Log.d("TEST", message);
//        int method = JSONHelper.parseMethod(message);
//        switch (method) {
//            case INFO:
//                processInfo(connection);
//                break;
//            case CONNECT:
//                processConnect(connection, message);
//                break;
//            case DISCONNECT:
//                processDisconnect(connection, message);
//                break;
//            default:
//                connection.sendMessage(JSONHelper.createSimpleAnswer(COMMAND_NOT_FOUND, "command not found"));
//        }
//    }
//
//    private void processInfo(Connection connection) throws JSONException {
//        connection.sendMessage(JSONHelper.createServerInfo(mServerInfo));
//    }
//
//    private void processConnect(Connection connection, String message) throws JSONException {
//        ParcelableClientInfo info = JSONHelper.parseUserInfo(message, mServerId);
//
//        if (isClientConnected(info.id)) {
//            connection.sendMessage(JSONHelper.createSimpleAnswer(ALREADY_CONNECTED, "already connected"));
//        } else {
//            ClientConnectionData data = new ClientConnectionData();
//            data.clientId = info.id;
//            data.connection = connection;
//            mClientsInfo.add(info);
//            mClientsConnectionData.add(data);
//            if (mOnClientListChanged != null) {
//                mOnClientListChanged.onClientListChanged(mClientsInfo);
//            }
//            connection.sendMessage(JSONHelper.createConnectionMessage(CONNECTED, "connected", mServerId));
//        }
//    }
//
//    private void processDisconnect(Connection connection, String message) throws JSONException {
//        String clientId = JSONHelper.parseClientId(message);
//        if (removeClient(clientId)) {
//            connection.sendMessage(JSONHelper.createConnectionMessage(DISCONNECT_FROM, "disconnected", mServerId));
//            connection.closeConnection();
//            removeClient(JSONHelper.parseClientId(message));
//        }
//    }
//
//    private boolean isClientConnected(String clientId) {
//        for (ClientConnectionData connection : mClientsConnectionData) {
//            if (connection.clientId.equals(clientId))
//                return true;
//        }
//
//        return false;
//    }
//
//    private void startConnection(Socket socket) {
//        Connection connection = new Connection(socket);
//        connection.setOnMessageReceivedListener(this);
//        try {
//            connection.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//
//    private ClientConnectionData getConnectionDataById(String clientId) {
//        for (ClientConnectionData data : mClientsConnectionData) {
//            if (data.clientId.equals(clientId))
//                return data;
//        }
//        return null;
//    }
//
//    private boolean removeClient(String clientId) {
//        for (ParcelableClientInfo info : mClientsInfo) {
//            if (info.id.equals(clientId)) {
//                mClientsInfo.remove(info);
//                mClientsConnectionData.remove(getConnectionDataById(info.id));
//                if(mOnClientListChanged != null) {
//                    mOnClientListChanged.onClientListChanged(mClientsInfo);
//                }
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public ArrayList<Connection> getAllBlockedConnections() {
//        ArrayList<Connection> connections = new ArrayList<Connection>();
//        for (ParcelableClientInfo info : mClientsInfo) {
//            if (info.isBlocked) {
//                Connection connection = getConnectionDataById(info.id).connection;
//                connections.add(connection);
//            }
//        }
//
//        return connections;
//    }
//
//
//    class ServerThread implements Runnable {
//
//        @Override
//        public void run() {
//            try {
//                mServerSocket = new ServerSocket(0);
//                mPort = mServerSocket.getLocalPort();
//                Log.d("TEST", "Server socket has opened");
//                while (!Thread.currentThread().isInterrupted()) {
//                    Socket socket = mServerSocket.accept();
//                    startConnection(socket);
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    mServerSocket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}

package ru.appkode.school.network;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.appkode.school.data.ConnectionData;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;

import static ru.appkode.school.util.JSONHelper.*;


/**
 * Created by lexer on 04.08.14.
 */
public class ClientConnection implements MessageReceiver.OnMessageReceive, ConnectionParams {

    private static final String TAG = "ClientConnection";

    public static final int SERVER_BLOCK = 0;
    public static final int SERVER_UNBLOCK = 1;

    public interface OnStatusChanged {
        public void OnStatusChanged(int status, String serverId, boolean isNeedStatusRefresh, ArrayList<String>[] lists);
    }

    public interface OnServerListChange {
        public void onServerListChange(ArrayList<ParcelableServerInfo> serversInfo);
    }

    public interface OnServerAction {
        public void onServerAction(String id, int action);
    }

    private ArrayList<ParcelableServerInfo> mServersInfo;
    private List<ConnectionData> mServersData;

    private MessageSender mSender;
    private MessageReceiver mReceiver;

    public ConnectionData mFirstConnectionData;
    public ConnectionData mSecondConnectionData;

    private OnServerListChange mOnServerListChange;
    private OnServerAction mOnServerAction;

    private ArrayList<String> mWhiteList;
    private ArrayList<String> mBlackList;

    private ParcelableClientInfo mClientInfo;

    public ClientConnection() {
        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mServersData = new ArrayList<ConnectionData>();

        mSender = new MessageSender();
        mReceiver = new MessageReceiver();
        while (!mReceiver.isInit()){};
        mReceiver.setOnMessageReceiveListener(this);

        mWhiteList = new ArrayList<String>();
        mBlackList = new ArrayList<String>();
    }

    public String getPort() {
        return String.valueOf(mReceiver.getPort());
    }


    public void connectToServer(String serverId, ParcelableClientInfo info) {
        ConnectionData data = getServerDataById(serverId);
        if (!info.isBlocked) {
            if (data != null) {
                Log.d(TAG, "mFirstConnectionData init = " + (mFirstConnectionData != null));
                if (mFirstConnectionData != null) {
                    disconnectFromServer(mFirstConnectionData.id, info, mFirstConnectionData.address, mFirstConnectionData.port);
                }
                connectToServer(serverId, info, data);
                mFirstConnectionData = data;
            }
        } else {
            if (data != null) {
                if (mSecondConnectionData != null) {
                    disconnectFromServer(mSecondConnectionData.id, info, mSecondConnectionData.address, mSecondConnectionData.port);
                }
                connectToServer(serverId, info, data);
                mSecondConnectionData = data;
            }
        }
    }

    private void connectToServer(String serverId, ParcelableClientInfo info, ConnectionData data) {
        ParcelableServerInfo infoForChange = getServerInfoById(serverId);
        if (infoForChange != null) {
            infoForChange.isLocked = true;
            infoForChange.isConnected = true;
            if (mOnServerListChange != null)
                mOnServerListChange.onServerListChange(mServersInfo);
        }
        HashMap<String, String> params = getBaseParams(info.id);
        params.put(NAME, info.name);
        params.put(LAST_NAME, info.lastName);
        params.put(GROUP, info.group);
        params.put(BLOCK_BY, info.blockedBy);
        try {
            mSender.sendMessage(createMessage(CONNECT, params), data.address, data.port);
        } catch (JSONException e) {
            Log.e(TAG, "error with create json with connect message " + e.getMessage());
        }
    }

    public void disconnectFromServer(String serverId, ParcelableClientInfo info) {
        ConnectionData data = getServerDataById(serverId);
        if (data != null) {
            disconnectFromServer(serverId, info, data.address, data.port);
        }
    }

    public void addServer(NsdServiceInfo nsdServiceInfo, ParcelableClientInfo info) {
        ParcelableServerInfo serverInfo = getServerInfoById(nsdServiceInfo.getServiceName());
        if (serverInfo != null) {
            checkServerAvailable(serverInfo, nsdServiceInfo.getHost());
            return;
        }
        String message;
        try {
            message = createMessage(INFO, getBaseParams(info.id));
        } catch (JSONException e) {
            Log.e(TAG, "error with creating info json " + e.getMessage());
            return;
        }

        mSender.sendMessage(message, nsdServiceInfo.getHost(), nsdServiceInfo.getPort());
    }

    public ArrayList<String> getWhiteList() {
        return mWhiteList;
    }

    public ArrayList<String> getBlackList() {
        return mBlackList;
    }

    public void setOnServerListChangeListener(OnServerListChange l) {
        mOnServerListChange = l;
    }

    public void setOnServerActionListener(OnServerAction l) {
        mOnServerAction = l;
    }

    public void setClientInfo(ParcelableClientInfo info) {
        mClientInfo = info;
    }

    public ArrayList<ParcelableServerInfo> getServersInfo() {
        return mServersInfo;
    }

    public boolean isConnected() {
        if (mFirstConnectionData != null || mSecondConnectionData != null)
            return true;
        else
            return false;
    }

    @Override
    public void onMessageReceive(String message, InetAddress address) {
        Log.d(TAG, "receive message = " + message + " from " + address.getHostName());
        try {
            String method = parseMethod(message);
            if (method.equals(CONNECT_CALLBACK)) {
                methodConnectCallback(message, address);
            } else if (method.equals(DISCONNECT_CALLBACK)) {
                methodDisconnectCallback(message, address);
            } else if (method.equals(INFO_CALLBACK)) {
                methodInfo(message, address);
            } else if (method.equals(BLOCK)) {
                methodBlock(message, address);
            } else if (method.equals(UNBLOCK)) {
                methodUnblock(message, address);
            } else if (method.equals(DISCONNECT_FROM)) {
                methodDisconnectFrom(message, address);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void methodConnectCallback(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            ParcelableServerInfo infoForChange = getServerInfoById(id);
            if (infoForChange != null) {
                infoForChange.isConnected = true;
                infoForChange.isLocked = false;
                if (mOnServerListChange != null) {
                    mOnServerListChange.onServerListChange(mServersInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "error parsing params in connect callback " + e.getMessage());
        }
    }

    private void methodDisconnectCallback(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            if (mFirstConnectionData != null && mFirstConnectionData.id.equals(id)) {
                mFirstConnectionData = null;
            }
//
//            if (mSecondConnectionData != null && mSecondConnectionData.id.equals(id)) {
//                mSecondConnectionData = null;
//            }

            ParcelableServerInfo infoForChange = getServerInfoById(id);
            if (infoForChange != null) {
                infoForChange.isConnected = false;
           //     infoForChange.isLocked = false;
                if (mOnServerListChange != null) {
                    mOnServerListChange.onServerListChange(mServersInfo);
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "error with parse params in disconnectCallback " + e.getMessage());
        }
    }

    private void methodInfo(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);

            String id = params.get(ID);
            String name = params.get(NAME);
            String secondName = params.get(SECOND_NAME);
            String lastName = params.get(LAST_NAME);
            String subject = params.get(SUBJECT);
            String port = params.get(PORT);

            ParcelableServerInfo info = new ParcelableServerInfo(lastName, name, secondName, subject, id);
            ConnectionData data = new ConnectionData(id, address, port);
            mServersInfo.add(info);
            mServersData.add(data);

            Log.d(TAG, info.toString());
            if (mOnServerListChange != null) {
                mOnServerListChange.onServerListChange(mServersInfo);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with parse params in info " + e.getMessage());
        }
    }


    private void methodBlock(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseBlockJson(message, mWhiteList, mBlackList);
            String id = params.get(ID);
            String port = params.get(PORT);

            ParcelableServerInfo infoForChange = getServerInfoById(id);
            if (infoForChange != null) {
                infoForChange.isConnected = true;
                infoForChange.isLocked = true;
            }

            if (mOnServerListChange != null) {
                mOnServerListChange.onServerListChange(mServersInfo);
            }

            if (mOnServerAction != null) {
                mOnServerAction.onServerAction(id, SERVER_BLOCK);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params in block " + e.getMessage());
        }
    }


    private void methodUnblock(String message, InetAddress address) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            String port = params.get(PORT);

            ParcelableServerInfo infoForChange = getServerInfoById(id);
            if (infoForChange != null) {
                infoForChange.isConnected = true;
                infoForChange.isLocked = false;
            }

            if (mOnServerListChange != null) {
                mOnServerListChange.onServerListChange(mServersInfo);
            }

            if (mOnServerAction != null) {
                mOnServerAction.onServerAction(id, SERVER_UNBLOCK);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params in unblock " + e.getMessage());
        }
    }

    private void methodDisconnectFrom(String message, InetAddress address) {
        if (mClientInfo == null) {
            throw new NullPointerException("set client info");
        }
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);
            String port = params.get(PORT);
            String fromId = params.get(FROM);

            ConnectionData data = getServerDataById(fromId);
            disconnectFromServer(data.id, mClientInfo, data.address, data.port);

            ParcelableServerInfo infoForChange = getServerInfoById(fromId);
            if (infoForChange != null) {
                infoForChange.isLocked = false;
                infoForChange.isConnected = false;
                if (mOnServerListChange != null) {
                    mOnServerListChange.onServerListChange(mServersInfo);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "error with parsing params in disconnectFrom " + e.getMessage());
        }
    }

    private void disconnectFromServer(String id, ParcelableClientInfo info, InetAddress address, String port) {
        HashMap<String, String> params = getBaseParams(info.id);
        ConnectionData data = getServerDataById(id);
        if (data != null && mFirstConnectionData != null && mFirstConnectionData.id.equals(data.id)
                && mSecondConnectionData != null) {
            mFirstConnectionData = mSecondConnectionData;
            mSecondConnectionData = null;
        }
        try {
            mSender.sendMessage(createMessage(DISCONNECT, params), address, port);
//            ParcelableServerInfo infoForChange = getServerInfoById(id);
//            if (infoForChange != null) {
//                infoForChange.isLocked = true;
//                if (mOnServerListChange != null) {
//                    mOnServerListChange.onServerListChange(mServersInfo);
//                }
//            }
        } catch (JSONException e) {
            Log.e(TAG, "error with create json with disconnect message" + e.getMessage());
        }
    }

    private void checkServerAvailable(final ParcelableServerInfo info, final InetAddress address) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!address.isReachable(3000)) {
                        mServersInfo.remove(info);
                        if (mOnServerListChange != null) {
                            mOnServerListChange.onServerListChange(mServersInfo);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public ParcelableServerInfo getServerInfoById(String id) {
        for (ParcelableServerInfo info : mServersInfo) {
            if (info.id.equals(id)) {
                return info;
            }
        }

        return null;
    }

    private ConnectionData getServerDataById(String serverId) {
        for (ConnectionData data : mServersData) {
            if (data.id.equals(serverId)) {
                return data;
            }
        }

        return null;
    }

    private HashMap<String, String> getBaseParams(String id) {
        HashMap<String, String> params = new HashMap<String, String>();

        params.put(ID, id);
        params.put(PORT, getPort());

        return params;
    }

//    public void connect(ParcelableServerInfo serverInfo, ParcelableClientInfo clientInfo) {
//        if (clientInfo == null) {
//            throw new IllegalArgumentException("client info is null, init it");
//        }
//
//        connectToServer(serverInfo, clientInfo);
//    }
//
//
//    public void disconnect(ParcelableServerInfo info, ParcelableClientInfo clientInfo) {
//        Log.d("TEST", "disconnect from " + info.name + " " + info.lastName);
//        disconnectFromServer(info, clientInfo);
//    }
//
//    public void askForServersInfo(List<NsdServiceInfo> infos) {
//        for (NsdServiceInfo info : infos) {
//            askForServerInfo(info);
//        }
//    }
//
//    public ArrayList<ParcelableServerInfo> getServersInfo() {
//        return mServersInfo;
//    }
//
//    public void askForServerInfo(final NsdServiceInfo serviceInfo) {
//        try {
//            ParcelableServerInfo serverInfo = getServerDataById(serviceInfo.getServiceName());
//            if (serverInfo != null && mOnServerInfoDownload != null && !mIsNamesSend) {
//                mOnServerInfoDownload.OnServerInfoDownload(serverInfo);
//                return;
//            }
//            Log.d("TEST", "ask info from " + serviceInfo.getHost() + " " + serviceInfo.getPort());
//            Connection connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
//            connection.setOnMessageReceivedListener(new Connection.OnMessageReceivedListener() {
//                @Override
//                public void onReceiveMessage(Connection connection, String message) {
//                    try {
//                        if (parseCode(message) == Server.INFO_CODE) {
//                            ParcelableServerInfo serverInfo = parseServerInfo(message);
//                            mServersData.add(new ServerConnectionData(serverInfo.id, serviceInfo));
//                            mServersInfo.add(serverInfo);
//                            if (mOnServerInfoDownload != null && !mIsNamesSend) {
//                                mOnServerInfoDownload.OnServerInfoDownload(serverInfo);
//                            }
//                            connection.sendMessage("END");
//                        } else {
//                            Log.e(TAG, "server error");
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            connection.start();
//            connection.sendMessage(createInfoRequest());
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void updateServerList(ArrayList<ParcelableServerInfo> list) {
//        mServersInfo = list;
//    }
//
//    public void setSendNames(boolean isSended) {
//        mIsNamesSend = isSended;
//    }
//
//    public boolean isConnected() {
//        return (mFirstConnectionData != null && mFirstConnectionData.connection != null && mFirstConnectionData.connection.isConnected()) ||
//                (mSecondConnectionData != null && mSecondConnectionData.connection != null && mSecondConnectionData.connection.isConnected());
//    }
//
////    public void removeServer(NsdServiceInfo serviceInfo) {
//
//    public void connectToServer(ParcelableServerInfo info, ParcelableClientInfo clientInfo) {
//        NsdServiceInfo serviceInfo;
//        ServerConnectionData dataForConnect = getServerInfoById(info.id);
//        try {
//            if (!clientInfo.isBlocked) {
//                if (mFirstConnectionData != null && !mFirstConnectionData.id.equals(info.id)) {
//                    sendDisconnect(mFirstConnectionData, clientInfo);
//                }
//                serviceInfo = dataForConnect.serviceInfo;
//                dataForConnect.connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
//                dataForConnect.connection.start();
//                dataForConnect.connection.setOnMessageReceivedListener(this);
//                dataForConnect.connection.sendMessage(createClientConnect(clientInfo));
//                mFirstConnectionData = dataForConnect;
//            } else {
//                if (mSecondConnectionData != null) {
//                    sendDisconnect(mSecondConnectionData, clientInfo);
//                }
//                serviceInfo = dataForConnect.serviceInfo;
//                dataForConnect.connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
//                dataForConnect.connection.start();
//                dataForConnect.connection.setOnMessageReceivedListener(this);
//                dataForConnect.connection.sendMessage(createClientConnect(clientInfo));
//                mSecondConnectionData = dataForConnect;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    //        String id = serviceInfo.getServiceName();
////        mServersData.remove(getServerInfoById(id));
////        mServersInfo.remove(getServerDataById(id));
////        if (mOnStatusChanged != null) {
////            mOnStatusChanged.OnStatusChanged();
////        }
//    public void checkConnectionsOnBlock(String id, ParcelableClientInfo clientInfo) {
//        if (mSecondConnectionData != null && mSecondConnectionData.id.equals(id)) {
//            sendDisconnect(mFirstConnectionData, clientInfo);
//            mFirstConnectionData = mSecondConnectionData;
//            mSecondConnectionData = null;
//        }
//    }
//
//    public void checkConnectionForUnblock(String id, ParcelableClientInfo clientInfo) {
//        if (mSecondConnectionData != null && mSecondConnectionData.id.equals(id)) {
//            sendDisconnect(mFirstConnectionData, clientInfo);
//            mFirstConnectionData = mSecondConnectionData;
//            mSecondConnectionData = null;
//        }
//    }
//
//
//    private ServerConnectionData getServerInfoById(String id) {
//        for (ServerConnectionData data : mServersData) {
//            if (data.id.equals(id)) {
//                return data;
//            }
//        }
//        return null;
//    }
//
//    private ParcelableServerInfo getServerDataById(String id) {
//        for (ParcelableServerInfo info : mServersInfo) {
//            if (info.id.equals(id)) {
//                return info;
//            }
//        }
//
//        return null;
//    }
//
//    public void disconnectFromServer(ParcelableClientInfo clientInfo) {
//        Log.d("TEST", "mFirstconndata null = " + (mFirstConnectionData == null));
//        Log.d("TEST", "mSecondConnectionData null = " + (mSecondConnectionData == null));
//        if (mFirstConnectionData != null) {
//            sendDisconnect(mFirstConnectionData, clientInfo);
//        }
//        if (mSecondConnectionData != null) {
//            sendDisconnect(mSecondConnectionData, clientInfo);
//        }
//    }
//
//    public void disconnectFromServer(ParcelableServerInfo info, ParcelableClientInfo clientInfo)  {
//        ServerConnectionData data = getServerInfoById(info.id);
//        if (data == mSecondConnectionData) {
//            mSecondConnectionData = null;
//        }
//        sendDisconnect(data, clientInfo);
//    }
//
//    private void sendDisconnect(ServerConnectionData data, ParcelableClientInfo clientInfo) {
//        try {
//            if (data != null && data.connection != null) {
//                data.connection.sendMessage(createClientDisconnect(clientInfo));
//                if (mOnStatusChanged != null) {
//                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECT_FROM, data.id, false, null);
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onReceiveMessage(Connection connection, String message) throws JSONException {
//        int code =  JSONHelper.parseCode(message);
//        Log.d("CLIENT_MESSAGE", message);
//        //200
//        if (mOnStatusChanged != null) {
//            String id = JSONHelper.parseServerId(message);
//            switch (code) {
//                case Server.CONNECTED:
//                    Log.d("CT", "connected to " + id);
//                    mOnStatusChanged.OnStatusChanged(code, id, true, null);
//                    break;
//                case Server.DISCONNECT_FROM:
//                    Log.d("CT", "disconnected from " + id);
//                    getServerInfoById(id).connection.closeConnection();
//                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECT_FROM, id, false, null);
//                    break;
//                case Server.DISCONNECT:
//                    Log.d("TEST", "delete code");
//                    getServerInfoById(id).connection.closeConnection();
//                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECT_FROM, id, true, null);
//                    break;
//                case Server.BLOCK_CODE:
//                    ArrayList[] lists = new ArrayList[2];
//                    lists[0] = JSONHelper.parseList(message, "white_list");
//                    lists[1] = JSONHelper.parseList(message, "black_list");
//                    mOnStatusChanged.OnStatusChanged(Server.BLOCK_CODE, id, true, lists);
//                    break;
//                case Server.UNBLOCK_CODE:
//                    mOnStatusChanged.OnStatusChanged(code, id, true, null);
//                     break;
//            }
//        }
//        if (code / 100 == 4) {
//            Log.e(TAG, "server error");
//        }
//    }
//
//    public void setOnStatusChangedListener(OnStatusChanged l) {
//        mOnStatusChanged = l;
//    }
//
//    public void setOnTeacherListChangedListener(OnTeacherListChanged l) {
//        mOnTeacherListChanged = l;
//    }
//
//    public void setOnServerInfoDownloadListener(OnServerInfoDownload l) {
//        mOnServerInfoDownload = l;
//    }
//
//    public interface OnServerInfoDownload {
//        public void OnServerInfoDownload(ParcelableServerInfo info);
//    }
}

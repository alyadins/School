package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    private static final String TAG = "ClientConnection";

    private ArrayList<ParcelableServerInfo> mServersInfo;

    private List<ServerConnectionData> mServers;

    private boolean mIsNamesSend = false;

    public interface OnStatusChanged {
        public void OnStatusChanged(int status, String serverId, boolean isNeedStatusRefresh, ArrayList<String>[] lists);
    }


    public interface OnTeacherListChanged {
        public void onTeacherListChanged(ClientConnection connection, List<ParcelableServerInfo> serversInfo);
    }

    private OnTeacherListChanged mOnTeacherListChanged;
    private OnStatusChanged mOnStatusChanged;
    private OnServerInfoDownload mOnServerInfoDownload;

    private ServerConnectionData mFirstConnectionData;
    private ServerConnectionData mSecondConnectionData;
    public ClientConnection() {
        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mServers = new ArrayList<ServerConnectionData>();
    }

    public void connect(ParcelableServerInfo serverInfo, ParcelableClientInfo clientInfo) {
        if (clientInfo == null) {
            throw new IllegalArgumentException("client info is null, init it");
        }

        connectToServer(serverInfo, clientInfo);
    }


    public void disconnect(ParcelableServerInfo info, ParcelableClientInfo clientInfo) {
        Log.d("TEST", "disconnect from " + info.name + " " + info.lastName);
        disconnectFromServer(info, clientInfo);
    }

    public void askForServersInfo(List<NsdServiceInfo> infos) {
        for (NsdServiceInfo info : infos) {
            askForServerInfo(info);
        }
    }

    public ArrayList<ParcelableServerInfo> getServersInfo() {
        return mServersInfo;
    }

    public void askForServerInfo(final NsdServiceInfo serviceInfo) {
        try {
            ParcelableServerInfo serverInfo = getServerInfoById(serviceInfo.getServiceName());
            if (serverInfo != null && mOnServerInfoDownload != null && !mIsNamesSend) {
                mOnServerInfoDownload.OnServerInfoDownload(serverInfo);
                return;
            }
            Log.d("TEST", "ask info from " + serviceInfo.getHost() + " " + serviceInfo.getPort());
            Connection connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
            connection.setOnMessageReceivedListener(new Connection.OnMessageReceivedListener() {
                @Override
                public void onReceiveMessage(Connection connection, String message) {
                    try {
                        if (parseCode(message) == Server.INFO_CODE) {
                            ParcelableServerInfo serverInfo = parseServerInfo(message);
                            mServers.add(new ServerConnectionData(serverInfo.serverId, serviceInfo));
                            mServersInfo.add(serverInfo);
                            if (mOnServerInfoDownload != null && !mIsNamesSend) {
                                mOnServerInfoDownload.OnServerInfoDownload(serverInfo);
                            }
                            connection.sendMessage("END");
                        } else {
                            Log.e(TAG, "server error");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            connection.start();
            connection.sendMessage(createInfoRequest());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateServerList(ArrayList<ParcelableServerInfo> list) {
        mServersInfo = list;
    }

    public void setSendNames(boolean isSended) {
        mIsNamesSend = isSended;
    }

    public boolean isConnected() {
        return (mFirstConnectionData != null && mFirstConnectionData.connection != null && mFirstConnectionData.connection.isConnected()) ||
                (mSecondConnectionData != null && mSecondConnectionData.connection != null && mSecondConnectionData.connection.isConnected());
    }

//    public void removeServer(NsdServiceInfo serviceInfo) {

    public void connectToServer(ParcelableServerInfo info, ParcelableClientInfo clientInfo) {
        NsdServiceInfo serviceInfo;
        ServerConnectionData dataForConnect = getServerById(info.serverId);
        try {
            if (!clientInfo.isBlocked) {
                if (mFirstConnectionData != null && !mFirstConnectionData.serverId.equals(info.serverId)) {
                    sendDisconnect(mFirstConnectionData, clientInfo);
                }
                serviceInfo = dataForConnect.serviceInfo;
                dataForConnect.connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
                dataForConnect.connection.start();
                dataForConnect.connection.setOnMessageReceivedListener(this);
                dataForConnect.connection.sendMessage(createClientConnect(clientInfo));
                mFirstConnectionData = dataForConnect;
            } else {
                if (mSecondConnectionData != null) {
                    sendDisconnect(mSecondConnectionData, clientInfo);
                }
                serviceInfo = dataForConnect.serviceInfo;
                dataForConnect.connection = new Connection(serviceInfo.getHost(), serviceInfo.getPort());
                dataForConnect.connection.start();
                dataForConnect.connection.setOnMessageReceivedListener(this);
                dataForConnect.connection.sendMessage(createClientConnect(clientInfo));
                mSecondConnectionData = dataForConnect;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //        String id = serviceInfo.getServiceName();
//        mServers.remove(getServerById(id));
//        mServersInfo.remove(getServerInfoById(id));
//        if (mOnStatusChanged != null) {
//            mOnStatusChanged.OnStatusChanged();
//        }
    public void checkConnectionsOnBlock(String serverId, ParcelableClientInfo clientInfo) {
        if (mSecondConnectionData != null && mSecondConnectionData.serverId.equals(serverId)) {
            sendDisconnect(mFirstConnectionData, clientInfo);
            mFirstConnectionData = mSecondConnectionData;
            mSecondConnectionData = null;
        }
    }

    public void checkConnectionForUnblock(String serverId, ParcelableClientInfo clientInfo) {
        if (mSecondConnectionData != null && mSecondConnectionData.serverId.equals(serverId)) {
            sendDisconnect(mFirstConnectionData, clientInfo);
            mFirstConnectionData = mSecondConnectionData;
            mSecondConnectionData = null;
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

    private ParcelableServerInfo getServerInfoById(String serverId) {
        for (ParcelableServerInfo info : mServersInfo) {
            if (info.serverId.equals(serverId)) {
                return info;
            }
        }

        return null;
    }

    public void disconnectFromServer(ParcelableClientInfo clientInfo) {
        Log.d("TEST", "mFirstconndata null = " + (mFirstConnectionData == null));
        Log.d("TEST", "mSecondConnectionData null = " + (mSecondConnectionData == null));
        if (mFirstConnectionData != null) {
            sendDisconnect(mFirstConnectionData, clientInfo);
        }
        if (mSecondConnectionData != null) {
            sendDisconnect(mSecondConnectionData, clientInfo);
        }
    }

    public void disconnectFromServer(ParcelableServerInfo info, ParcelableClientInfo clientInfo)  {
        ServerConnectionData data = getServerById(info.serverId);
        if (data == mSecondConnectionData) {
            mSecondConnectionData = null;
        }
        sendDisconnect(data, clientInfo);
    }

    private void sendDisconnect(ServerConnectionData data, ParcelableClientInfo clientInfo) {
        try {
            if (data != null && data.connection != null) {
                data.connection.sendMessage(createClientDisconnect(clientInfo));
                if (mOnStatusChanged != null) {
                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECTED, data.serverId, false, null);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMessage(Connection connection, String message) throws JSONException {
        int code =  JSONHelper.parseCode(message);
        Log.d("CLIENT_MESSAGE", message);
        //200
        if (mOnStatusChanged != null) {
            String serverId = JSONHelper.parseServerId(message);
            switch (code) {
                case Server.CONNECTED:
                    Log.d("CT", "connected to " + serverId);
                    mOnStatusChanged.OnStatusChanged(code, serverId, true, null);
                    break;
                case Server.DISCONNECTED:
                    Log.d("CT", "disconnected from " + serverId);
                    getServerById(serverId).connection.closeConnection();
                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECTED, serverId, false, null);
                    break;
                case Server.DISCONNECT:
                    Log.d("TEST", "delete code");
                    getServerById(serverId).connection.closeConnection();
                    mOnStatusChanged.OnStatusChanged(Server.DISCONNECTED, serverId, true, null);
                    break;
                case Server.BLOCK_CODE:
                    ArrayList[] lists = new ArrayList[2];
                    lists[0] = JSONHelper.parseList(message, "white_list");
                    lists[1] = JSONHelper.parseList(message, "black_list");
                    mOnStatusChanged.OnStatusChanged(Server.BLOCK_CODE, serverId, true, lists);
                    break;
                case Server.UNBLOCK_CODE:
                    mOnStatusChanged.OnStatusChanged(code, serverId, true, null);
                     break;
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

    public void setOnServerInfoDownloadListener(OnServerInfoDownload l) {
        mOnServerInfoDownload = l;
    }

    public interface OnServerInfoDownload {
        public void OnServerInfoDownload(ParcelableServerInfo info);
    }
}

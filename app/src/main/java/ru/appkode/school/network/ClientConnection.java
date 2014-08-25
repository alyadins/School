package ru.appkode.school.network;

import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jmdns.ServiceInfo;

import ru.appkode.school.data.ConnectionData;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;

import static ru.appkode.school.util.JSONHelper.*;


public class ClientConnection implements MessageReceiver.OnMessageReceive, ConnectionParams {

    private static final String TAG = "ClientConnection";

    public static final int SERVER_BLOCK = 0;
    public static final int SERVER_UNBLOCK = 1;

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
        while (!mReceiver.isInit()){}
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

    public void setServers(ArrayList<ServiceInfo> infos, ParcelableClientInfo clientInfo) {
        deleteServers(infos);
        for (ServiceInfo info : infos) {
            String message;
            try {
                message = createMessage(INFO, getBaseParams(clientInfo.id));
            } catch (JSONException e) {
                Log.e(TAG, "error with creating info json " + e.getMessage());
                return;
            }

            mSender.sendMessage(message, info.getAddress(), info.getPort());
        }
    }

    private void deleteServers(ArrayList<ServiceInfo> infos) {
        ArrayList<ParcelableServerInfo> availableServers = new ArrayList<ParcelableServerInfo>();
        for (ServiceInfo info : infos) {
            ParcelableServerInfo serverInfo = getServerInfoById(info.getName());
            if (serverInfo != null) {
                availableServers.add(serverInfo);
            }
        }

        mServersInfo.clear();
        mServersInfo.addAll(availableServers);
        if (mOnServerListChange != null) {
            mOnServerListChange.onServerListChange(mServersInfo);
        }
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
        return mFirstConnectionData != null || mSecondConnectionData != null;
    }

    @Override
    public void onMessageReceive(String message, InetAddress address) {
        Log.d(TAG, "receive message = " + message + " from " + address.getHostName());
        try {
            String method = parseMethod(message);
            if (method.equals(CONNECT_CALLBACK)) {
                methodConnectCallback(message);
            } else if (method.equals(DISCONNECT_CALLBACK)) {
                methodDisconnectCallback(message);
            } else if (method.equals(INFO_CALLBACK)) {
                methodInfo(message, address);
            } else if (method.equals(BLOCK)) {
                methodBlock(message);
            } else if (method.equals(UNBLOCK)) {
                methodUnblock(message);
            } else if (method.equals(DISCONNECT_FROM)) {
                methodDisconnectFrom(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void methodConnectCallback(String message) {
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

    private void methodDisconnectCallback(String message) {
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

            ParcelableServerInfo info = getServerInfoById(id);

            if (info == null) {
                info = new ParcelableServerInfo(lastName, name, secondName, subject, id);
                mServersInfo.add(info);
            } else {
                info.name = name;
                info.secondName = secondName;
                info.subject = subject;
                info.lastName = lastName;
            }

            ConnectionData data = getServerDataById(id);
            if (data == null) {
                data = new ConnectionData(id, address, port);
                mServersData.add(data);
            } else {
                data.address = address;
                data.port = port;
            }

            if (mOnServerListChange != null) {
                mOnServerListChange.onServerListChange(mServersInfo);
            }
        } catch (JSONException e) {
            Log.e(TAG, "error with parse params in info " + e.getMessage());
        }
    }


    private void methodBlock(String message) {
        try {
            HashMap<String, String> params = parseBlockJson(message, mWhiteList, mBlackList);
            String id = params.get(ID);

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


    private void methodUnblock(String message) {
        try {
            HashMap<String, String> params = parseParams(message);
            String id = params.get(ID);

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

    private void methodDisconnectFrom(String message) {
        if (mClientInfo == null) {
            throw new NullPointerException("set client info");
        }
        try {
            HashMap<String, String> params = parseParams(message);
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
        } catch (JSONException e) {
            Log.e(TAG, "error with create json with disconnect message" + e.getMessage());
        }
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


}

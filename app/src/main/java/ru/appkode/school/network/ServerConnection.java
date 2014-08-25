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
        while (!mReceiver.isInit()){}
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
            if (!info.isBlockedByOther)
                unblock(info);
            disconnect(mServerInfo.id, info);
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
            } else if (blockBy.equals(mServerInfo.id)) {
                info.isBlocked = true;
            } else {
                info.isBlocked = true;
                info.blockedBy = blockBy;
                info.isBlockedByOther = true;
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
        if (info.isBlockedByOther) {
            ConnectionData dataForDisconnect = getClientConnectionDataById(info.id);
            if (dataForDisconnect != null) {
                disconnect(info.blockedBy, info);
            }
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
                    infoForChange.isBlockedByOther = false;
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
        if (info.isBlockedByOther) {
            ConnectionData dataForDisconnect = getClientConnectionDataById(info.id);
            if (dataForDisconnect != null) {
                disconnect(info.blockedBy, info);
            }
        }
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
                    infoForChange.isBlockedByOther = false;
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


    private void disconnect(String id, ParcelableClientInfo info) {
        ConnectionData data = getClientConnectionDataById(info.id);
        try {
            HashMap<String, String> params = getBaseParams();
            params.put(FROM, id);
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

}

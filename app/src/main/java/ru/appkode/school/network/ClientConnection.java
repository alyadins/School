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

import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.data.TeacherInfo;


/**
 * Created by lexer on 04.08.14.
 */
public class ClientConnection implements NsdManager.ResolveListener, NsdManager.DiscoveryListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    private Context mContext;
    private NsdManager mNsdManager;
    private Connection mConnection;
    private List<ServerInfo> mServersInfo;

    private boolean mIsDiscoveryStarted = false;

    private OnTeacherListChanged mOnTeacherListChanged;

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
            askForTeacherInfo(serviceInfo);
        } else {
            Log.d("TEST", serviceName + " already added");
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

    private TeacherInfo askForTeacherInfo(final NsdServiceInfo serviceInfo) {
        final TeacherInfo info = new TeacherInfo();
        try {
            Connection connection = new Connection(new Socket(serviceInfo.getHost(), serviceInfo.getPort()));
            connection.setOnMessageReceivedListener(new Connection.OnMessageReceivedListener() {
                @Override
                public void onReceiveMessage(Connection connection, String message) {
                    addService(serviceInfo, parseTeacherInfoJson(message));
                    connection.sendMessage("END");
                }
            });
            connection.start();
            connection.sendMessage("info");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return info;
    }

    private void addService(NsdServiceInfo serviceInfo, TeacherInfo teacherInfo) {
        Log.d("TEST", "adding serivce teacherInfo" + serviceInfo.getServiceName());
        ServerInfo info = new ServerInfo();
        info.serviceInfo = serviceInfo;
        info.teacherInfo = teacherInfo;
        mServersInfo.add(info);

        if (mOnTeacherListChanged != null) {
            mOnTeacherListChanged.onTeacherListChanged(this, mServersInfo);
        }
    }

    private TeacherInfo parseTeacherInfoJson(String message) {
        try {
            JSONObject object = new JSONObject(message);
            String name = object.getString("name");
            String secondName = object.getString("second_name");
            String lastName = object.getString("last_name");
            String subject = object.getString("subject");
            return  new TeacherInfo(lastName, name, secondName, subject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<ServerInfo> getServersInfo() {
        return mServersInfo;
    }

    public void setOnTeacherListChangedListener(OnTeacherListChanged l) {
        mOnTeacherListChanged = l;
    }

    public interface OnTeacherListChanged {
        public void onTeacherListChanged(ClientConnection connection, List<ServerInfo> serversInfo);
    }
}

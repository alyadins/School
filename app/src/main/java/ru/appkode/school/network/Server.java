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

import ru.appkode.school.data.TeacherInfo;

/**
 * Created by lexer on 01.08.14.
 */
public class Server implements Connection.OnMessageReceivedListener, NsdManager.RegistrationListener {

    public static final String SERVICE_TYPE = "_http._tcp.";
    private Context mContext;
    private ServerSocket mServerSocket = null;
    private int mPort = -1;

    private String mServerName;

    private List<Connection> connections;

    private TeacherInfo mTeacherInfo;

    private NsdManager mNsdManager;
    private boolean mIsRegistered;

    public Server(Context context, String serverName) {
        mServerName = serverName;
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        connections = new ArrayList<Connection>();
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
        connections.add(connection);
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
        if (message.equals("info")) {
            connection.sendMessage(getTeacherInfoJson());
        } else {
            connection.sendMessage(createErrorJson(400, "command not found"));
        }
    }

    private String getTeacherInfoJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("code", 200);
            json.put("name", mTeacherInfo.name);
            json.put("second_name", mTeacherInfo.secondName);
            json.put("last_name", mTeacherInfo.lastName);
            json.put("subject", mTeacherInfo.subject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    private String createErrorJson(int code, String message) {
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

}

package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by lexer on 06.08.14.
 */
public class ClientNameRegistrator implements NsdManager.RegistrationListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    private String mServiceName;
    private NsdManager mNsdManager;

    private boolean mIsRegisered;

    public ClientNameRegistrator( NsdManager manager) {
        mNsdManager = manager;
    }

    public void register(String name) {
        mServiceName = name;
        Thread registrationThread = new Thread(new ClientNameThread());
        registrationThread.start();
    }

    private void registerService(NsdServiceInfo info) {
        mNsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, this);
    }


    class ClientNameThread implements Runnable {

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(0);
                NsdServiceInfo serviceInfo  = new NsdServiceInfo();
                serviceInfo.setPort(socket.getLocalPort());
                serviceInfo.setServiceName(mServiceName);
                serviceInfo.setServiceType(SERVICE_TYPE);

                registerService(serviceInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void unregisterService() {
        if (mIsRegisered)
            mNsdManager.unregisterService(this);
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "onRegistrationFailed");
        mIsRegisered = false;
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "onUnregistrationFailed");
        mIsRegisered = false;
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        mIsRegisered = true;
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        mIsRegisered = false;
        Log.d("TEST", "onServiceUnregistered");
    }
}

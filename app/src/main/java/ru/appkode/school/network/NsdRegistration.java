package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Created by lexer on 10.08.14.
 */
public class NsdRegistration implements NsdManager.RegistrationListener {

    public static final String TAG = "NsdRegistration";

    public static final String SERVICE_TYPE = "_http._tcp.";

    private NsdManager mManager;
    private String mName;
    private int mPort = -1;
    private boolean mIsRegistered = false;
    private boolean mIsNeedNewRegistration = false;

    public NsdRegistration(NsdManager mManager, int port) {
        this(mManager);
        mPort = port;
    }

    public NsdRegistration(NsdManager mManager) {
        this.mManager = mManager;
    }

    public void setName(String name) {
        mName = name;
        mIsNeedNewRegistration = true;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public boolean  isRegistered() {
        return mIsRegistered;
    }

    public void start() {
        register();
    }

    public void stop() {
        if (mIsRegistered)
            mManager.unregisterService(this);
    }

    private void register() {
        if (mIsRegistered)
            mManager.unregisterService(this);
        else
            registerService();
    }

    private void registerService() {
        mManager.registerService(getServiceInfo(), NsdManager.PROTOCOL_DNS_SD, this);
    }

    private NsdServiceInfo getServiceInfo() {
        if (mPort == -1) {
            throw new IllegalArgumentException("Set port in constructor or setPort");
        }

        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(mPort);
        serviceInfo.setServiceName(mName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        return serviceInfo;
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d(TAG, "registration fail " + serviceInfo.getServiceName());
        mIsRegistered = false;
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d(TAG, "unregistration fail " + serviceInfo.getServiceName());
        mIsRegistered = false;
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "registered " + serviceInfo.getServiceName());
        mIsRegistered = true;
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "unregistered " + serviceInfo.getServiceName());
        mIsRegistered = false;
        if (mIsNeedNewRegistration) {
            registerService();
            mIsNeedNewRegistration = false;
        }
    }
}

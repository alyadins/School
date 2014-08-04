package ru.appkode.school.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Created by lexer on 04.08.14.
 */
public class ClientConnection implements NsdManager.ResolveListener, NsdManager.DiscoveryListener {

    public static final String SERVICE_TYPE = "_http._tcp.";

    private Context mContext;
    private NsdManager mNsdManager;
    private Connection connection;

    private boolean isDiscovered = false;

    public ClientConnection(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void discover() {
        if (!isDiscovered)
            mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
    }

    public void stopDiscover() {
        if (isDiscovered) {
            mNsdManager.stopServiceDiscovery(this);
        }
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        isDiscovered = false;
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        isDiscovered = false;
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        Log.d("TEST", "discovery started");
        isDiscovered = true;
    }

    @Override
     public void onDiscoveryStopped(String serviceType) {
        isDiscovered = false;
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.d("TEST", "service found " + serviceInfo.getServiceName());
        mNsdManager.resolveService(serviceInfo, this);
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {

    }

    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.d("TEST", "resolve failed " + errorCode);
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        addPossibleConnection(serviceInfo);
    }

    private void addPossibleConnection(NsdServiceInfo serviceInfo) {

    }
}

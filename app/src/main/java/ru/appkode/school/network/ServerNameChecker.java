package ru.appkode.school.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lexer on 06.08.14.
 */
public class ServerNameChecker implements NsdManager.DiscoveryListener, NsdManager.ResolveListener {

    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final int DELAY = 3000;

    private Context mContext;
    private NsdManager mNsdManager;

    private List<String> mRegisteredNames;
    private NameChecked mNameChecked;

    private String mNameForCheck;

    public ServerNameChecker(Context context) {
        this.mContext = context;
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        mRegisteredNames = new ArrayList<String>();
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopDiscover();
            }
        }, DELAY);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.d("TEST", "check = " + mNameForCheck);
        for (String n : mRegisteredNames) {
            Log.d("TEST", n);
        }
        if (mNameChecked != null) {
            mNameChecked.onNameChecked(!isServerAdded(mNameForCheck));
        }
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        mNsdManager.resolveService(serviceInfo, this);
    }


    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        if (isServerAdded(name)) {
            deleteServer(name);
        }
    }

    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        if (!isServerAdded(name)) {
            mRegisteredNames.add(name);
        }
    }

    public void isNameFree(String name) {
        mNameForCheck = name;
        startDiscover();
    }

    private void startDiscover() {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
    }

    private void stopDiscover() {
        mNsdManager.stopServiceDiscovery(this);
    }

    private boolean isServerAdded(String name) {
        for (String n : mRegisteredNames) {
            Log.d("TEST", n);
            if (n.equals(name))
                return true;
        }

        return false;
    }

    private void deleteServer(String name) {
        for (String n : mRegisteredNames) {
            if (n.equals(name))
                mRegisteredNames.remove(n);
        }
    }

    public void setNameCheckedListener(NameChecked l) {
        mNameChecked = l;
    }

    public interface NameChecked {
        public void onNameChecked(boolean isFree);
    }
}

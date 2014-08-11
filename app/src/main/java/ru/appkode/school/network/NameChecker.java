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
public class NameChecker implements NsdManager.DiscoveryListener {

    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final int DELAY = 1000;

    private Context mContext;
    private NsdManager mNsdManager;

    private List<String> mRegisteredNames;
    private NameChecked mNameChecked;

    private String mNameForCheck;

    public NameChecker(Context context) {
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
        mRegisteredNames.clear();
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        if (!isServerAdded(name)) {
            mRegisteredNames.add(name);
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        if (isServerAdded(name)) {
            deleteServer(name);
        }
    }


    public void isNameFree(String name) {
        mNameForCheck = name;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mNameChecked != null) {
                    mNameChecked.onNameChecked(!isServerAdded(mNameForCheck));
                }
            }
        }, DELAY);
    }

    public void startDiscover() {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this);
    }

    public void stopDiscover() {
        mNsdManager.stopServiceDiscovery(this);
    }

    private boolean isServerAdded(String name) {
        for (String n : mRegisteredNames) {
            Log.d("TEST", "name = " + name);
            if (n.equals(name))
                return true;
        }

        return false;
    }

    private void deleteServer(String name) {
        String nameForRemove = "";
        for (String n : mRegisteredNames) {
            if (n.equals(name))
                nameForRemove = n;
        }
        mRegisteredNames.remove(nameForRemove);
    }

    public void setNameCheckedListener(NameChecked l) {
        mNameChecked = l;
    }

    public interface NameChecked {
        public void onNameChecked(boolean isFree);
    }
}

package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by lexer on 10.08.14.
 */
public class NsdName{

    public static final String SERVICE_TYPE = "_http._tcp.";
    private static final int SERVER = 0;
    private static final int CLIENT = 1;
    private static final int UNKNOWN = 2;

    private NsdManager mManager;

    private List<NsdServiceInfo> mDiscoveredServices;
    private List<NsdServiceInfo> mResolvedServices;
    private Queue<NsdServiceInfo> mResolveQueue;

    private boolean mIsDiscoveryStarted = false;

    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;

    public NsdName(NsdManager mManager) {
        this.mManager = mManager;
        mDiscoveredServices = new ArrayList<NsdServiceInfo>();
        mResolvedServices = new ArrayList<NsdServiceInfo>();
        initDiscoveryListener();
        initResolveListener();
    }


    public void start() {
        mManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stop() {
        mManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public boolean isDiscoveryStarted() {
        return mIsDiscoveryStarted;
    }

    public boolean isNameFree(String name) {
        for (NsdServiceInfo si : mDiscoveredServices) {
            if (si.getServiceName().equals(name)) {
                return false;
            }
        }

        return true;
    }

    public List<NsdServiceInfo> getDiscoveredServices() {
        return mDiscoveredServices;
    }

    public void resolveServices(Queue<NsdServiceInfo> resolveQueue) {
        mResolveQueue = resolveQueue;
        resolveNext();
    }


    public List<NsdServiceInfo> getResolvedServices() {
        return mResolvedServices;
    }


    public void resolveServers() {
        Queue<NsdServiceInfo> infos = new ArrayDeque<NsdServiceInfo>();
        for (NsdServiceInfo si : mDiscoveredServices) {
            if (getTypeOfService(si.getServiceName()) == SERVER) {
                infos.add(si);
            }
        }
        resolveServices(infos);
    }

    public boolean isResolveQueueEmpty() {
        return mResolveQueue.isEmpty();
    }

    private int getTypeOfService(String id) {
        if (id.substring(0, 4).equals("serv")) {
            return SERVER;
        } if (id.substring(0, 6).equals("client")) {
            return CLIENT;
        } else {
            return UNKNOWN;
        }
    }

    private boolean isServiceDiscovered(NsdServiceInfo serviceInfo) {
        return isContainsService(serviceInfo, mDiscoveredServices);
    }

    private boolean isServiceResolved(NsdServiceInfo serviceInfo) {
        return isContainsService(serviceInfo, mResolvedServices);
    }

    private boolean isContainsService(NsdServiceInfo serviceInfo, List<NsdServiceInfo> servicesInfo) {
        for (NsdServiceInfo si : servicesInfo) {
            if (si.getServiceName().equals(serviceInfo.getServiceName()))
                return true;
        }

        return false;
    }

    private boolean removeDiscoveredService(NsdServiceInfo serviceInfo) {
        return removeServiceFrom(serviceInfo, mDiscoveredServices);
    }

    private boolean removeResolvedService(NsdServiceInfo serviceInfo) {
        return removeServiceFrom(serviceInfo, mResolvedServices);
    }

    private boolean removeServiceFrom(NsdServiceInfo serviceInfo, List<NsdServiceInfo> servicesInfo) {
        for (NsdServiceInfo si : servicesInfo) {
            if (si.getServiceName().equals(serviceInfo.getServiceName())) {
                servicesInfo.remove(si);
                return true;
            }
        }
        return false;
    }


    private void initDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d("TEST", "discovery started");
                mIsDiscoveryStarted = true;
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (!isServiceDiscovered(serviceInfo)) {
                    Log.d("TEST", "found service " + serviceInfo.getServiceName());
                    mDiscoveredServices.add(serviceInfo);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                removeDiscoveredService(serviceInfo);
                removeResolvedService(serviceInfo);
            }
        };
    }

    private void initResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                if (errorCode == 0) {
                    mManager.resolveService(serviceInfo, mResolveListener);
                    return;
                }

                resolveNext();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                resolveNext();
                Log.d("TEST", "resolced " + serviceInfo.getServiceName());
                mResolvedServices.add(serviceInfo);
            }
        };
    }

    private void resolveNext() {
        if (!mResolveQueue.isEmpty()) {
            NsdServiceInfo info = mResolveQueue.poll();
            if (!isServiceResolved(info)) {
                mManager.resolveService(info, mResolveListener);
            }
        }
    }

}

package ru.appkode.school.network;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by lexer on 10.08.14.
 */
public class NsdName{

    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final int SERVER = 0;
    public static final int CLIENT = 1;
    private static final int UNKNOWN = 2;

    private static final int CAPACITY = 50;

    private NsdManager mManager;

    private List<NsdServiceInfo> mDiscoveredServices;
    private List<NsdServiceInfo> mResolvedServices;
    private BlockingQueue<NsdServiceInfo> mResolveQueue;

    private boolean mIsDiscoveryStarted = false;
    private boolean mIsResolving = false;

    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;

    private int mType;

    public NsdName(NsdManager mManager, int type) {
        this.mManager = mManager;
        mDiscoveredServices = new ArrayList<NsdServiceInfo>();
        mResolvedServices = new ArrayList<NsdServiceInfo>();
        mResolveQueue = new ArrayBlockingQueue<NsdServiceInfo>(CAPACITY);
        mType = type;
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

    public List<NsdServiceInfo> getResolvedServices() {
        return mResolvedServices;
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
                Log.d("TEST", "discovery start fail code = " + errorCode);
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d("TEST", "discovery stop fail code = " + errorCode);
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d("TEST", "discovery started");
                mIsDiscoveryStarted = true;
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d("TEST", "discovery stoped");
                mIsDiscoveryStarted = false;
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d("TEST", "found " + serviceInfo.getServiceName());
                if (!isServiceDiscovered(serviceInfo)) {
                    mDiscoveredServices.add(serviceInfo);
                    if (getTypeOfService(serviceInfo.getServiceName()) == SERVER && mType != SERVER) {
                        if (mResolveQueue.isEmpty() && !mIsResolving) {
                            mResolveQueue.add(serviceInfo);
                            resolveNext();
                        } else {
                            mResolveQueue.add(serviceInfo);
                        }
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d("TEST", "lost " + serviceInfo.getServiceName());
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
                Log.d("TEST", "resolved " + serviceInfo.getServiceName());
                mResolvedServices.add(serviceInfo);
            }
        };
    }

    private void resolveNext() {
        if (!mIsResolving)
            mIsResolving = true;
        if (!mResolveQueue.isEmpty()) {
            NsdServiceInfo info = mResolveQueue.poll();
            Log.d("TEST", "try to resolve " + info.getServiceName());
            mManager.resolveService(info, mResolveListener);
        } else {
            mIsResolving = false;
        }
    }

}

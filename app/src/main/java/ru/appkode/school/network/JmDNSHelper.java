package ru.appkode.school.network;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Created by lexer on 24.08.14.
 */
public class JmDNSHelper {

    private static final String TAG = "JmDNSRegistaration";

    private final String TYPE = "_alwx._tcp.local.";

    private String mName;
    private Context mContext;
    private JmDNS mJmDNS;
    private ServiceInfo mServiceInfo;
    private ServiceListener mServiceListener;
    private List<ServiceInfo> mServices;

    public JmDNSHelper(Context mContext) {
        this.mContext = mContext;

        try {
            WifiManager wifi = (WifiManager) mContext.getSystemService(android.content.Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            int intaddr = wifiInfo.getIpAddress();

            byte[] byteaddr = new byte[]{
                    (byte) (intaddr & 0xff),
                    (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff),
                    (byte) (intaddr >> 24 & 0xff)
            };
            InetAddress addr = InetAddress.getByAddress(byteaddr);
            mJmDNS = JmDNS.create(addr);
        } catch (UnknownHostException e) {
            Log.e(TAG, "unknown host in constructor " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "io exception in constructor " + e.getMessage());
        }

        initDiscoveryListener();

        mServices = new ArrayList<ServiceInfo>();
    }

    public void start(String name ,int port) {
        try {
            mServiceInfo = ServiceInfo.create(TYPE, name, port, name);
            mJmDNS.registerService(mServiceInfo);
        } catch (IOException e) {
            Log.d(TAG, "Error in JmDNS initialization: " + e);
        }
    }

    public void stop() {
        if (mJmDNS != null) {
            if (mServiceListener != null) {
                mJmDNS.removeServiceListener(TYPE, mServiceListener);
                mServiceListener = null;
            }
            mJmDNS.unregisterAllServices();
        }
    }

    private void initDiscoveryListener() {
        mServiceListener = new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent serviceEvent) {
                Log.d(TAG, "service added " + serviceEvent.getName());
                ServiceInfo info = mJmDNS.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
                mServices.add(info);
            }

            @Override
            public void serviceRemoved(ServiceEvent serviceEvent) {
                Log.d(TAG, "service removed " + serviceEvent.getName());
            }

            @Override
            public void serviceResolved(ServiceEvent serviceEvent) {
                Log.d(TAG, "service resolved " + serviceEvent.getName());
                mJmDNS.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName());
            }
        };
    }
}

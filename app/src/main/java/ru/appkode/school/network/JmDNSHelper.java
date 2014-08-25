package ru.appkode.school.network;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class JmDNSHelper {

    private static final String TAG = "JmDNSHelper";

    private final String TYPE = "_http._tcp.local.";

    private JmDNS mJmDNS;
    private ServiceInfo mServiceInfo;
    private ServiceListener mServiceListener;

    public JmDNSHelper(final Context context, final String name, final String port) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] addresses = getLocalIpAddress();
                    String address = "";
                    for (String s : addresses) {
                        if (s.substring(0, 9).equals("192.168.1")) {
                            address = s;
                            break;
                        }
                    }
                    InetAddress addr = InetAddress.getByName(address);
//                    WifiManager wifi = (WifiManager) mContext.getSystemService(android.content.Context.WIFI_SERVICE);
//                    WifiInfo wifiInfo = wifi.getConnectionInfo();
//                    int intaddr = wifiInfo.getIpAddress();
//
//                    byte[] byteaddr = new byte[]{
//                            (byte) (intaddr & 0xff),
//                            (byte) (intaddr >> 8 & 0xff),
//                            (byte) (intaddr >> 16 & 0xff),
//                            (byte) (intaddr >> 24 & 0xff)
//                    };
//                    InetAddress addr = InetAddress.getByAddress(byteaddr);
                    mJmDNS = JmDNS.create(addr);
                    Log.d(TAG, "jmdns created " + addr.getHostName());
                    mServiceInfo = ServiceInfo.create(TYPE, name, Integer.parseInt(port), name);
                    mJmDNS.registerService(mServiceInfo);
                    Log.d(TAG, "registered " + mServiceInfo.getName() + "port = " + mServiceInfo.getPort());
                } catch (UnknownHostException e) {
                    Log.e(TAG, "unknown host in constructor " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "io exception in constructor " + e.getMessage());
                }
            }
        }).start();
    }

    public void stop() {
        Log.d(TAG, "stop");
        if (mJmDNS != null) {
            if (mServiceListener != null) {
                mJmDNS.removeServiceListener(TYPE, mServiceListener);
                mServiceListener = null;
            }
            mJmDNS.unregisterAllServices();
        }
    }

    public void findServers(final OnServicesFound l) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServiceInfo[] services = mJmDNS.list(TYPE);
                for (ServiceInfo info : services) {
                    Log.d(TAG, "found " + info.getName());
                }
                ArrayList<ServiceInfo> serverServices = new ArrayList<ServiceInfo>();
                for (ServiceInfo info : services) {
                    if (info.getName().substring(0, 4).equals("serv"))
                        serverServices.add(info);
                }
                if (l != null) {
                    l.onServicesFound(serverServices);
                }
            }
        }).start();

    }

    public String[] getLocalIpAddress()
    {
        ArrayList<String> addresses = new ArrayList<String>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        addresses.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "socket exception " + ex.getMessage());
        }
        return addresses.toArray(new String[0]);
    }


    public interface OnServicesFound {
        public void onServicesFound(ArrayList<ServiceInfo> infos);
    }
}

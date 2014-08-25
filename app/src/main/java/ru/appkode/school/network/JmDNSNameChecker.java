package ru.appkode.school.network;

import android.util.Log;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class JmDNSNameChecker {
    private static final String TAG = "JmDNSNameChecker";

    private final String TYPE = "_http._tcp.local.";

    private JmDNS mJmDns;

    public JmDNSNameChecker() {
    }

    public void isNameFree(final String name, final OnNameCheckListener onNameCheck) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isFree = true;
                    mJmDns = JmDNS.create();
                    ServiceInfo[] infos = mJmDns.list(TYPE);
                    for (ServiceInfo info : infos) {
                        if (info.getName().equals(name)) {
                            isFree = false;
                            break;
                        }
                    }
                    if (onNameCheck != null) {
                        onNameCheck.onNameCheck(isFree);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "mJmDns create error " + e.getMessage());
                }
            }
        }).start();
    }

    public interface OnNameCheckListener {
        public void onNameCheck(boolean isFree);
    }
}

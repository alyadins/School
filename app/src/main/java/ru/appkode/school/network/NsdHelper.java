/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.appkode.school.network;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;

import java.util.List;

public class NsdHelper {


//    NsdManager.ResolveListener mResolveListener;
//    NsdManager.DiscoveryListener mDiscoveryListener;
//    NsdManager.RegistrationListener mRegistrationListener;
//
//    public static final String TAG = "NsdHelper";
//    public List<String> serviceNames;
//
//
//    public String mServiceName;
//    NsdServiceInfo mService;
//
//    public NsdHelper(Context context) {
//
//    }
//
//    public void initializeNsd() {
//        initializeResolveListener();
//        initializeDiscoveryListener();
//        initializeRegistrationListener();
//
//        //mNsdManager.init(mContext.getMainLooper(), this);
//
//    }
//
//    public void initializeDiscoveryListener() {
//        mDiscoveryListener = new NsdManager.DiscoveryListener() {
//
//            @Override
//            public void onDiscoveryStarted(String regType) {
//                Log.d(TAG, "Service discovery started");
//            }
//
//            @Override
//            public void onServiceFound(NsdServiceInfo service) {
//                Log.d("TEST", service.getHost() + "  " + service.getServiceName() + " " + service.getPort());
//                mNsdManager.resolveService(service, mResolveListener);
//            }
//
//            @Override
//            public void onServiceLost(NsdServiceInfo service) {
//                Log.e(TAG, "service lost" + service);
//                if (mService == service) {
//                    mService = null;
//                }
//            }
//
//            @Override
//            public void onDiscoveryStopped(String serviceType) {
//                Log.i(TAG, "Discovery stopped: " + serviceType);
//            }
//
//            @Override
//            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
//                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
//                mNsdManager.stopServiceDiscovery(this);
//            }
//
//            @Override
//            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
//                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
//                mNsdManager.stopServiceDiscovery(this);
//            }
//        };
//    }
//
//    public void initializeResolveListener() {
//        mResolveListener = new NsdManager.ResolveListener() {
//
//            @Override
//            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
//                Log.e(TAG, "Resolve failed" + errorCode);
//            }
//
//            @Override
//            public void onServiceResolved(NsdServiceInfo serviceInfo) {
//
//                Log.d("TEST", "resolve " + serviceInfo.getServiceName() + "  " + serviceInfo.getHost() + "  " + serviceInfo.getPort());
//
//                if (serviceInfo.getServiceName().equals("")) {
//                    Log.d(TAG, "Same IP.");
//                    return;
//                }
//                mService = serviceInfo;
//            }
//        };
//    }
//
//    public void initializeRegistrationListener() {
//        mRegistrationListener = new NsdManager.RegistrationListener() {
//
//            @Override
//            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
//                Log.d("TEST", "name = " + NsdServiceInfo.getServiceName() + "  port = " + NsdServiceInfo.getPort());
//            }
//
//            @Override
//            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
//            }
//
//            @Override
//            public void onServiceUnregistered(NsdServiceInfo arg0) {
//            }
//
//            @Override
//            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
//            }
//
//        };
//    }
//
//    public void setServiceName(String name) {
//        mServiceName = name;
//    }
//
//
//
//
//    public void stopDiscovery() {
//        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
//    }
//
//    public NsdServiceInfo getChosenServiceInfo() {
//        return mService;
//    }

}

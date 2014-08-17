package ru.appkode.school.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.activity.StudentActivity;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.ClientConnection;
import ru.appkode.school.network.FakeServer;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.Server;
import ru.appkode.school.util.BlockHelper;
import ru.appkode.school.util.ClientSharedPreferences;

/**
 * Created by lexer on 10.08.14.
 */
public class ClientService extends Service implements ClientConnection.OnStatusChanged, ClientConnection.OnServerInfoDownload {
    public static final int NOTIF_ID = 1;

    public static final String ACTION = "action";
    public static final String BROADCAST_ACTION = "ru.appkode.school.clientbroadcast";

    //Actions
    public static final int START = 0;
    public static final int IS_FREE = 1;
    public static final int STOP = 2;
    public static final int GET_NAMES = 4;
    public static final int CONNECT = 6;
    public static final int DISCONNECT = 7;
    public static final int BLOCK = 9;
    public static final int UNBLOCK = 10;
    public static final int STATUS = 11;
    public static final int CHANGE_NAME = 12;
    public static final int WHITE_LIST = 13;

    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String IS_INIT = "is_init";
    public static final String WHITE_LIST_PARAM = "white_list";

    private NsdManager mManager;

    private NsdRegistration mNsdRegistration;

    private NsdName mNsdName;
    private ClientConnection mClientConnection;
    private FakeServer mFakeServer;

    private ArrayList<ParcelableServerInfo> mServersInfo;
    private List<NsdServiceInfo> mResolvedServers;

    private boolean mIsInfoSend = false;
    private ParcelableClientInfo mClientInfo;
    private ArrayList<String> mWhiteList;

    private ArrayList<String> mBlackList;
    private boolean mIsFirstLaunch = true;
    private ClientSharedPreferences mClientSharedPreferences;

    private BlockHelper mBlockHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager, NsdName.CLIENT);

        mFakeServer = new FakeServer();
        mClientConnection = new ClientConnection();
        mClientConnection.setOnStatusChangedListener(this);
        mClientConnection.setOnServerInfoDownloadListener(this);
        mNsdName.start();

        lockWifi();

        this.startForeground();

        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mResolvedServers = new ArrayList<NsdServiceInfo>();

        mClientInfo = new ParcelableClientInfo();

        mClientSharedPreferences = new ClientSharedPreferences(this);
        mBlockHelper = new BlockHelper(this);
    }
    private void startForeground() {
        startForeground(NOTIF_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SP", "start sp");
        if (mIsFirstLaunch) {
            if (mClientSharedPreferences.checkSharedPreferences(mClientInfo)) {
                Log.d("SP", "sp reade " + mClientInfo.clientId + " " + mClientInfo.name + " " + mClientInfo.lastName);
                actionStart(mClientInfo);
            }
            mIsFirstLaunch = false;
        }

        if (intent != null) {
            int action = intent.getIntExtra(ACTION, -1);
            runAction(action, intent);
        }

        return START_STICKY;
    }

    private void runAction(int action, Intent intent) {

        if (action <= IS_FREE) {
            ParcelableClientInfo clientInfo = intent.getParcelableExtra(NAME);
            switch (action) {
                case START:
                    actionStart(clientInfo);
                    break;
                case IS_FREE:
                    actionIsFree(clientInfo);
                    break;
            }
        } else if (action >= CONNECT && action <= DISCONNECT) {
            ParcelableServerInfo serverInfo = intent.getParcelableExtra(NAME);
            switch (action) {
                case CONNECT:
                    actionConnect(serverInfo);
                    break;
                case DISCONNECT:
                    actionDisconnect(serverInfo);
                    break;
            }
        } else switch (action) {
            case STATUS:
                actionStatus();
                break;
            case GET_NAMES:
                actionGetNames();
                break;
            case STOP:
                actionStop();
                break;
            case CHANGE_NAME:
                actionChangeName();
                break;
            case WHITE_LIST:
                actionWhiteList();
        }
    }

    private void actionStart(ParcelableClientInfo clientInfo) {
        if (!mNsdRegistration.isRegistered()) {
            startService(clientInfo);
        } else {
            mNsdRegistration.stop();
            while (mNsdRegistration.isRegistered()) {}
            startService(clientInfo);
        }
    }

    private void startService(ParcelableClientInfo clientInfo) {
        if (mClientConnection != null && mClientInfo.clientId != null) {
            mClientConnection.disconnectFromServer(mClientInfo);
        }

        mClientInfo = clientInfo;
        mClientSharedPreferences.writeSharedPreferences(mClientInfo);

        while (mFakeServer.getPort() == -1) {} // wait start server
        int port = mFakeServer.getPort();

        mNsdRegistration.setName(mClientInfo.clientId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();

        mClientConnection.setClientInfo(mClientInfo);

        sendSimpleBroadcast(START, "started");
    }

    private void actionStop() {
        mNsdRegistration.stop();
        mNsdName.stop();
    }

    private void actionIsFree(final ParcelableClientInfo clientInfo) {
        for (int i = 0; i < 10; i++) {
            if (!mNsdName.isDiscoveryStarted()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (i == 10) {
                throw new InternalError("discovery isn't started");
            } else {
                break;
            }
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isNameFree = mNsdName.isNameFree(clientInfo.clientId, mClientInfo.clientId);
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(CODE, IS_FREE);
                intent.putExtra(MESSAGE, isNameFree);
                sendBroadcast(intent);
            }
        }, 500);
    }

    private void actionConnect(ParcelableServerInfo serverInfo) {
        mClientConnection.connect(serverInfo);
    }

    private void actionDisconnect(ParcelableServerInfo info) {
        mClientConnection.disconnect(info, mClientInfo);
    }

    private void actionGetNames() {
        mResolvedServers = mNsdName.getResolvedServices();
        mServersInfo.clear();
        mIsInfoSend = false;
        if (mResolvedServers.size() == 0) {
            sendNames(mServersInfo);
        }

        mClientConnection.askForServersInfo(mResolvedServers);

        if (!mNsdName.isDiscoveryStarted()) {
            mNsdName.start();
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<ParcelableServerInfo> infos = mClientConnection.getServersInfo();
                if (!mIsInfoSend) {
                    sendNames(infos);
                }

            }
        }, 5000);
    }

    private void actionStatus() {
        Intent intent = new Intent(BROADCAST_ACTION);

        if (mClientInfo != null && mClientInfo.isInit() && mServersInfo != null) {
            intent.putExtra(CODE, STATUS);
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAMES, mServersInfo);
            intent.putExtra(NAME, mClientInfo);
        } else {
            intent.putExtra(CODE, STATUS);
            intent.putExtra(IS_INIT, false);
        }
        sendBroadcast(intent);
    }

    private void actionChangeName() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, CHANGE_NAME);
        if (mClientInfo != null && mClientInfo.isInit()) {
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAME, mClientInfo);
        } else {
            intent.putExtra(IS_INIT, false);
        }

        sendBroadcast(intent);
    }

    private void actionWhiteList() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, WHITE_LIST);
        if (mWhiteList != null && mWhiteList.size() > 0) {
            intent.putExtra(IS_INIT, mClientInfo.isBlocked);
            intent.putStringArrayListExtra(WHITE_LIST_PARAM, mWhiteList);
        }

        sendBroadcast(intent);
    }


    private void sendSimpleBroadcast(int code, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, code);
        intent.putExtra(MESSAGE, message);
        sendBroadcast(intent);
    }

    @Override
    public void OnServerInfoDownload(ParcelableServerInfo info) {
        mServersInfo.add(info);

        Log.d("TEST", "onServerInfoDownload = " + info.name + " " + info.lastName);
        if (mResolvedServers.size() == mServersInfo.size()) {
            mIsInfoSend = true;
            sendNames(mServersInfo);
        }
    }



    private void sendNames(ArrayList<ParcelableServerInfo> infos) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putParcelableArrayListExtra(NAMES, infos);
        intent.putExtra(CODE, GET_NAMES);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void OnStatusChanged(final int status, final String serverId, boolean isNeedStatusRefresh, ArrayList<String>[] lists) {
        ParcelableServerInfo info;
        switch (status) {
            case Server.BLOCK_CODE:
                mClientInfo.isBlocked = true;
                mClientInfo.blockedBy = serverId;
                info = getServerInfoById(serverId);
                info.isLocked = true;
                updateNotification();
                mClientSharedPreferences.writeSharedPreferences(mClientInfo);

                mWhiteList = lists[0];
                mBlackList =  lists[1];
                mBlockHelper.setWhiteList(mWhiteList);
                mBlockHelper.setBlackList(mBlackList);
                mBlockHelper.block();

                actionStatus();
                break;
            case Server.UNBLOCK_CODE:
                mClientInfo.blockedBy = "none";
                mClientInfo.isBlocked = false;

                unblockAllServers();
                updateNotification();

                mClientSharedPreferences.writeSharedPreferences(mClientInfo);
                mBlockHelper.unBlock();

                actionStatus();
                break;
            case Server.CONNECTED:
                info = getServerInfoById(serverId);
                info.isConnected = true;
                if (isNeedStatusRefresh)
                    actionStatus();
                break;
            case Server.DISCONNECTED:
                info = getServerInfoById(serverId);
                info.isConnected = false;
                if (isNeedStatusRefresh)
                    actionStatus();
                break;
        }
    }

    private void unblockAllServers() {
        for (ParcelableServerInfo info : mServersInfo) {
            info.isLocked = false;
        }
    }



    private ParcelableServerInfo getServerInfoById(String serverId) {
        for (ParcelableServerInfo info : mServersInfo) {
            if (info.serverId.equals(serverId))
                return info;
        }

        return null;
    }



    private void updateNotification() {
        Notification notification = getNotification();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIF_ID, notification);
    }

    private Notification getNotification() {
        CharSequence title = getText(R.string.app_name);
        String message;
        String[] status = getResources().getStringArray(R.array.status);
        int imageRes;
        if (mClientInfo != null && mClientInfo.isBlocked) {
            ParcelableServerInfo info = getServerInfoById(mClientInfo.blockedBy);
            message = status[1] + " " + info.name + " " + info.secondName;
            imageRes = R.drawable.lock_small;
        } else  {
            message = status[0];
            imageRes = R.drawable.unlock_small;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, new Intent(this, StudentActivity.class), 0);

        return new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(imageRes)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setTicker(message)
                .build();
    }

    private void lockWifi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        lock.acquire();
    }


    @Override
    public void onDestroy() {
        Log.d("TEST", "onDestroy");
        mNsdRegistration.stop();
        mNsdName.stop();
        super.onDestroy();
    }
}

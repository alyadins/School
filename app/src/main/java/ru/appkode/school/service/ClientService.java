package ru.appkode.school.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.activity.StudentActivity;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.ClientConnection;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.ServerConnection;
import ru.appkode.school.util.BlockHelper;
import ru.appkode.school.util.ClientSharedPreferences;
import ru.appkode.school.util.FavouriteHelper;

/**
 * Created by lexer on 10.08.14.
 */
public class ClientService extends Service implements  ClientConnection.OnServerListChange, ClientConnection.OnServerAction {
    public static final String TAG = "ClientService";
    public static final int NOTIF_ID = 1;

    public static final String ACTION = "action";
    public static final String BROADCAST_ACTION = "ru.appkode.school.clientbroadcast";
    private static final String BLOCK_TIME = "block_time";
    private static final String BLOCK_BY_IN_TIME = "block_by_in_time";
    private static final int MAX_TIME = 40000;

    //Actions
    public static final int START = 0;
    public static final int IS_FREE = 1;
    public static final int STOP = 2;
    public static final int GET_NAMES = 4;
    public static final int CONNECT = 6;
    public static final int DISCONNECT = 7;
    public static final int FAVOURITE = 8;
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
    public static final String IS_CONNECTED = "is_connected";
    public static final String WHITE_LIST_PARAM = "white_list";

    private NsdManager mManager;



    private NsdRegistration mNsdRegistration;

    private NsdName mNsdName;
    private ClientConnection mClientConnection;

    private ParcelableClientInfo mClientInfo;

    private ArrayList<String> mWhiteList;
    private ArrayList<String> mBlackList;

    private boolean mIsFirstLaunch = true;

    private ClientSharedPreferences mClientSharedPreferences;
    private BlockHelper mBlockHelper;

    private FavouriteHelper mFavouriteHelper;
    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager, NsdName.CLIENT);
        mNsdName.start();

        lockWifi();

       this.startForeground();

        mClientInfo = new ParcelableClientInfo();
        mClientSharedPreferences = new ClientSharedPreferences(this);

        mBlockHelper = new BlockHelper(this);
       // mFavouriteHelper = new FavouriteHelper();

        mClientConnection = new ClientConnection();
        mClientConnection.setOnServerListChangeListener(this);
        mClientConnection.setOnServerActionListener(this);

        startCheckBlockTime();
    }

    private void startForeground() {
        startForeground(NOTIF_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mIsFirstLaunch) {
            if (mClientSharedPreferences.checkSharedPreferences(mClientInfo)) {
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
        } else if (action >= CONNECT && action <= FAVOURITE) {
            ParcelableServerInfo serverInfo = intent.getParcelableExtra(NAME);
            switch (action) {
                case CONNECT:
                    actionConnect(serverInfo);
                    break;
                case DISCONNECT:
                    actionDisconnect(serverInfo);
                    break;
                case FAVOURITE:
                    actionFavourite(serverInfo);
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
        mClientInfo = clientInfo;
        mClientSharedPreferences.writeSharedPreferences(mClientInfo);

        mNsdRegistration.setName(mClientInfo.id);
        mNsdRegistration.setPort(Integer.parseInt(mClientConnection.getPort()));
        mNsdRegistration.start();

        mClientConnection.setClientInfo(mClientInfo);

        sendSimpleBroadcast(START, "started");
    }

    private void actionStop() {
        mNsdRegistration.stop();
        mNsdName.stop();
    }

    private void actionIsFree(final ParcelableClientInfo clientInfo) {
        boolean isNameFree = mNsdName.isNameFree(clientInfo.id, mClientInfo.id);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, IS_FREE);
        intent.putExtra(MESSAGE, isNameFree);
        sendBroadcast(intent);
    }

    private void actionConnect(ParcelableServerInfo serverInfo) {
        mClientConnection.connectToServer(serverInfo.id, mClientInfo);
    }

    private void actionDisconnect(ParcelableServerInfo info) {
        mClientConnection.disconnectFromServer(info.id, mClientInfo);
    }

    private void actionFavourite(ParcelableServerInfo serverInfo) {
//        ParcelableServerInfo info = getServerInfoById(serverInfo.id);
//        if (info.isFavourite) {
//            info.isFavourite = false;
//            mFavouriteHelper.remove(serverInfo.id);
//            mFavouriteHelper.save();
//        } else {
//            info.isFavourite = true;
//            mFavouriteHelper.add(serverInfo.id);
//            mFavouriteHelper.save();
//        }
//
//        actionStatus();
    }

    private void actionGetNames() {
        List<NsdServiceInfo> resolvedServers = mNsdName.getResolvedServices();

        for (NsdServiceInfo info : resolvedServers)
            mClientConnection.addServer(info, mClientInfo);
    }

    private void actionStatus() {
        Intent intent = new Intent(BROADCAST_ACTION);

        if (mClientInfo != null && mClientInfo.isInit()) {
            intent.putExtra(CODE, STATUS);
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAMES, mClientConnection.getServersInfo());
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
            intent.putExtra(IS_CONNECTED, mClientConnection.isConnected());
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
        } else {
            intent.putExtra(IS_INIT, false);
        }

        sendBroadcast(intent);
    }

    @Override
    public void onServerListChange(ArrayList<ParcelableServerInfo> serversInfo) {
        actionStatus();
    }

    @Override
    public void onServerAction(String id, int action) {
        Log.d(TAG, "onServerAction " + id);
        switch (action) {
            case ClientConnection.SERVER_BLOCK:
                mClientInfo.isBlocked = true;
                mClientInfo.blockedBy = id;
                updateNotification();

                mClientSharedPreferences.writeSharedPreferences(mClientInfo);

                mWhiteList = mClientConnection.getWhiteList();
                mBlackList =  mClientConnection.getBlackList();
                mBlockHelper.setWhiteList(mWhiteList);
                mBlockHelper.setBlackList(mBlackList);
                mBlockHelper.block();

                saveBlockTime(id);
                actionStatus();
                break;
            case ClientConnection.SERVER_UNBLOCK:
                mClientInfo.isBlocked = false;
                mClientInfo.blockedBy = "none";
                updateNotification();

                mClientSharedPreferences.writeSharedPreferences(mClientInfo);
                mBlockHelper.unBlock();

                clearBlockTime();
                actionStatus();
                break;
        }
    }

    private void sendSimpleBroadcast(int code, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, code);
        intent.putExtra(MESSAGE, message);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


//    @Override
//    public void OnStatusChanged(final int status, final String serverId, boolean isNeedStatusRefresh, ArrayList<String>[] lists) {
//        ParcelableServerInfo info;
//        switch (status) {
//            case ServerConnection.BLOCK_CODE:
//                block(serverId, lists);
//                break;
//            case ServerConnection.UNBLOCK_CODE:
//                unblock(serverId);
//                break;
//            case ServerConnection.CONNECTED:
//                info = getServerInfoById(serverId);
//                info.isConnected = true;
//                if (isNeedStatusRefresh)
//                    actionStatus();
//                break;
//            case ServerConnection.DISCONNECT_FROM:
//                info = getServerInfoById(serverId);
//                info.isConnected = false;
//                info.isLocked = false;
//                if (isNeedStatusRefresh)
//                    actionStatus();
//                break;
//        }
//    }
//
//    private void block(String serverId, ArrayList<String>[] lists) {
//        ParcelableServerInfo info;
//        mClientConnection.checkConnectionsOnBlock(serverId, mClientInfo);
//        mClientInfo.isBlocked = true;
//        mClientInfo.blockedBy = serverId;
//        info = getServerInfoById(serverId);
//        info.isLocked = true;
//        updateNotification();
//        mClientSharedPreferences.writeSharedPreferences(mClientInfo);
//
//        mWhiteList = lists[0];
//        mBlackList =  lists[1];
//        mBlockHelper.setWhiteList(mWhiteList);
//        mBlockHelper.setBlackList(mBlackList);
//        mBlockHelper.block();
//
//        saveBlockTime(serverId);
//
//        actionStatus();
//
//    }

//    private void unblock(String serverId) {
//        mClientInfo.blockedBy = "none";
//        mClientInfo.isBlocked = false;
//
//        mClientConnection.checkConnectionForUnblock(serverId, mClientInfo);
//        unblockAllServers();
//        updateNotification();
//        mClientSharedPreferences.writeSharedPreferences(mClientInfo);
//        mBlockHelper.unBlock();
//
//        clearBlockTime();
//
//        actionStatus();
//
//    }
//
//    private void unblockAllServers() {
//        for (ParcelableServerInfo info : mServersInfo) {
//            info.isLocked = false;
//        }
//    }

    private void saveBlockTime(String serverId) {
        SharedPreferences.Editor editor = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE).edit();
        editor.putLong(BLOCK_TIME, System.currentTimeMillis());
        editor.putString(BLOCK_BY_IN_TIME, serverId);
        editor.commit();
    }

    private void clearBlockTime() {
        SharedPreferences.Editor editor = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE).edit();
        editor.putLong(BLOCK_TIME, -1);
        editor.commit();
    }

//    private ParcelableServerInfo getServerInfoById(String serverId) {
//        for (ParcelableServerInfo info : mServersInfo) {
//            if (info.id.equals(serverId))
//                return info;
//        }
//
//        return null;
//    }


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
            ParcelableServerInfo info = mClientConnection.getServerInfoById(mClientInfo.blockedBy);
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

    private void startCheckBlockTime() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d("BLOCKTIME", "startCheckBlockTime");
                SharedPreferences preferences = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
                long blockTime = preferences.getLong(BLOCK_TIME, -1);
                String blockId = preferences.getString(BLOCK_BY_IN_TIME, "none");
                Log.d("BLOCKTIME", "blocktime = " + blockTime + " currentTime = " + System.currentTimeMillis());
                Log.d("BLOCKTIME", "blockid = " + blockId);
                if (blockTime != -1) {
                    long currentTime = System.currentTimeMillis();
                    Log.d("BLOCKTIME", "diff = " + (currentTime - blockTime));
                    if (currentTime - blockTime > MAX_TIME && !blockId.equals("none")) {
                    //   unblock(blockId);
                    }
                }
            }
        }, 0, ServerService.BLOCK_PERIOD);
    }


    @Override
    public void onDestroy() {
        Log.d("TEST", "onDestroy");
        mNsdRegistration.stop();
        mNsdName.stop();
        super.onDestroy();
    }
}

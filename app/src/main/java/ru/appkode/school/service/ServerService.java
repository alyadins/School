package ru.appkode.school.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.activity.StudentActivity;
import ru.appkode.school.activity.TeacherActivity;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.Server;
import ru.appkode.school.util.AppListHelper;
import ru.appkode.school.util.ServerSharedPreferences;

/**
 * Created by lexer on 10.08.14.
 */
public class ServerService extends Service implements Server.OnClientListChanged {

    public static final int NOTIF_ID = 245;
    public static final String ACTION = "action";

    //Actions
    public static final int START = 0;
    public static final int IS_NAME_FREE = 1;
    public static final int STOP = 2;
    public static final int CLIENTS_CONNECTED = 3;
    public static final int BLOCK = 4;
    public static final int UNBLOCK = 5;
    public static final int DELETE = 6;
    public static final int GET_CLIENTS = 7;
    public static final int STATUS = 8;
    public static final int CHANGE_NAME = 9;


    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";

    //Answers
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String IS_CLIENTS_CONNECTED = "isClientsConnected";
    public static final String IS_INIT = "is_init";

    public static final String BROADCAST_ACTION = "ru.appkode.school.serverbroadcast";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;
    private ServerSharedPreferences mSharedPreferences;

    private Server mServer;
    private ParcelableServerInfo mServerInfo;

    private AppListHelper mAppListHelper;
    private List<String> mWhiteList;
    private List<String> mBlackList;

    private boolean mIsFirstLaunch = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager, NsdName.SERVER);
        mNsdName.start();
        mSharedPreferences = new ServerSharedPreferences(this);
        mServer = new Server();
        mServer.start();
        mServer.setOnClientListChangedListener(this);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        lock.acquire();

        mServerInfo = new ParcelableServerInfo();

        mAppListHelper = new AppListHelper(this);
        mWhiteList = mAppListHelper.getList(AppListHelper.WHITE_LIST);
        mBlackList = mAppListHelper.getList(AppListHelper.BLACK_LIST);
        this.startForeground();
    }

    private void startForeground() {
        startForeground(NOTIF_ID, getNotification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsFirstLaunch) {
            Log.d("sharedPreferences", "check SP on start");
            if (mSharedPreferences.checkSharedPreferences(mServerInfo)) {
                Log.d("sharedPreferences", "onStart  id = " + mServerInfo.serverId + " name = " + mServerInfo.name);
                actionStart(mServerInfo);
            }
            mIsFirstLaunch = false;
        }

        if (intent != null) {
            int command = intent.getIntExtra(ACTION, -1);
            runAction(command, intent);
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runAction(int action, Intent intent) {
        if (action <= IS_NAME_FREE) {
            ParcelableServerInfo info = intent.getParcelableExtra(NAME);
            switch (action) {
                case START:
                    mServerInfo = info;
                    actionStart(mServerInfo);
                    break;
                case IS_NAME_FREE:
                    actionIsNameFree(info);
                    break;
            }
        } else if (action >= BLOCK && action <= DELETE){
            List<ParcelableClientInfo> names = intent.getParcelableArrayListExtra(NAMES);
            switch (action) {
                case BLOCK:
                    actionBlock(names);
                    break;
                case UNBLOCK:
                    actionUnblock(names);
                    break;
                case DELETE:
                    actionDelete(names);
                    break;
            }
        } else switch (action) {
            case GET_CLIENTS:
                actionGetClients();
                break;
            case STOP:
                actionStop();
                break;
            case STATUS:
                actionStatus();
                break;
            case CLIENTS_CONNECTED:
                actionClientsConnected();
                break;
            case CHANGE_NAME:
                actionChangeName();
        }
    }

    //Actions methods
    private void actionStart(ParcelableServerInfo info) {
        mServerInfo = info;
        if (!mNsdRegistration.isRegistered()) {
            startService(info);
        } else {
            mNsdRegistration.stop();
            while (mNsdRegistration.isRegistered()) {}
            mServer.disconnectAllClients();
            startService(info);
        }
    }

    private void startService(ParcelableServerInfo info) {
        Log.d("TEST", "start service " + info.name + " " + info.lastName);
        mSharedPreferences.writeSharedPreferences(info);
        mServer.setServerInfo(info);
        while (mServer.getPort() == -1) {}//Wait start server
        int port = mServer.getPort();

        mNsdRegistration.setName(info.serverId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();


        sendSimpleBroadCast(START, "started");
    }

    private void actionIsNameFree(final ParcelableServerInfo info) {
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
        //wait 0.5 seconds for discover
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isNameFree = mNsdName.isNameFree(info.serverId, mServerInfo.serverId);
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(CODE, IS_NAME_FREE);
                intent.putExtra(MESSAGE, isNameFree);
                sendBroadcast(intent);
            }
        }, 500);
    }

    private void actionStop() {
        mNsdRegistration.stop();
        sendSimpleBroadCast(STOP, "stop");
    }

    private void actionBlock(List<ParcelableClientInfo> names) {
        mServer.block(names, mWhiteList, mBlackList);
        sendSimpleBroadCast(BLOCK, "blocked");
    }

    private void actionUnblock(List<ParcelableClientInfo> names) {
        mServer.unBlock(names);
        sendSimpleBroadCast(UNBLOCK, "unblocked");
    }

    private void actionDelete(List<ParcelableClientInfo> names) {
        mServer.disconnect(names);
        sendSimpleBroadCast(DELETE, "deleted");
    }

    private void actionGetClients() {
        sendBroadCastClientsList(mServer.getClientsInfo());
    }

    private void actionStatus() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, STATUS);
        if (mServerInfo != null && mServerInfo.isInit() && mServer != null) {
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAME, mServerInfo);
            intent.putExtra(NAMES, mServer.getClientsInfo());
        } else {
            intent.putExtra(IS_INIT, false);
        }

        sendStickyBroadcast(intent);
    }

    private void actionClientsConnected() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, CLIENTS_CONNECTED);
        intent.putExtra(IS_CLIENTS_CONNECTED, mServer.getClientsInfo().size() > 0);
        sendBroadcast(intent);
    }

    private void actionChangeName() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, CHANGE_NAME);
        if (mServerInfo != null && mServerInfo.isInit()) {
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAME, mServerInfo);
        } else {
            intent.putExtra(IS_INIT, false);
        }

        sendBroadcast(intent);
    }

    @Override
    public void onClientListChanged(ArrayList<ParcelableClientInfo> clientsInfo) {
        actionStatus();
    }

    private void sendBroadCastClientsList(ArrayList<ParcelableClientInfo> clientsInfo) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putParcelableArrayListExtra(NAMES, clientsInfo);
        intent.putExtra(CODE, GET_CLIENTS);
        sendBroadcast(intent);
    }
    private void sendSimpleBroadCast(int code, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, code);
        intent.putExtra(MESSAGE, message);
        sendBroadcast(intent);
    }


    private Notification getNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, new Intent(this, TeacherActivity.class), 0);
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.app_icon)
                .setContentText(getString(R.string.teacher_mode))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .build();
    }


    @Override
    public void onDestroy() {
        Log.d("TEST", "on destroy");
        mNsdName.stop();
        mNsdRegistration.stop();
        super.onDestroy();
    }
}

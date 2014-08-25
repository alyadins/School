package ru.appkode.school.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.activity.TeacherActivity;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.JmDNSHelper;
import ru.appkode.school.network.JmDNSNameChecker;
import ru.appkode.school.network.ServerConnection;
import ru.appkode.school.util.AppListHelper;
import ru.appkode.school.util.ServerSharedPreferences;

public class ServerService extends Service implements ServerConnection.OnClientListChanged {

    private static final String TAG = "ServerService";

    public static final int NOTIF_ID = 245;
    public static final String ACTION = "action";

    public static final int BLOCK_PERIOD = 10000;
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


    private JmDNSHelper mJmDNSHelper;

    private ServerConnection mServerConnection;
    private ParcelableServerInfo mServerInfo;

    private AppListHelper mAppListHelper;
    private ArrayList<String> mWhiteList;
    private ArrayList<String> mBlackList;
    private ServerSharedPreferences mSharedPreferences;

    private boolean mIsFirstLaunch = true;

    @Override
    public void onCreate() {
        super.onCreate();

        mServerConnection = new ServerConnection();
        mServerConnection.setOnClientListChangedListener(this);

        lockWifi();

        mServerInfo = new ParcelableServerInfo();

        mAppListHelper = new AppListHelper(this);
        mWhiteList = mAppListHelper.getList(AppListHelper.WHITE_LIST);
        mBlackList = mAppListHelper.getList(AppListHelper.BLACK_LIST);

        mServerConnection.setWhiteList(mWhiteList);
        mServerConnection.setBlackList(mBlackList);

        mSharedPreferences = new ServerSharedPreferences(this);

     //   startBlockedThread();

        this.startForeground();
    }

    private void startForeground() {
        startForeground(NOTIF_ID, getNotification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsFirstLaunch) {
            if (mSharedPreferences.checkSharedPreferences(mServerInfo)) {
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
        startService(info);
    }

    private void startService(ParcelableServerInfo info) {
        mSharedPreferences.writeSharedPreferences(info);

        mJmDNSHelper = new JmDNSHelper(this, info.id, mServerConnection.getPort());

        mServerConnection.setServerInfo(info);

        sendSimpleBroadCast(START, "started");
    }

    private void actionIsNameFree(final ParcelableServerInfo info) {
        JmDNSNameChecker checker = new JmDNSNameChecker();
        checker.isNameFree(info.id, new JmDNSNameChecker.OnNameCheckListener() {
            @Override
            public void onNameCheck(boolean isFree) {
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(CODE, IS_NAME_FREE);
                intent.putExtra(MESSAGE, isFree);
                sendBroadcast(intent);
            }
        });
    }

    private void actionStop() {
        mJmDNSHelper.stop();
        sendSimpleBroadCast(STOP, "stop");
    }

    private void actionBlock(List<ParcelableClientInfo> names) {
        mServerConnection.blockClients(names);
    }

    private void actionUnblock(List<ParcelableClientInfo> names) {
        mServerConnection.unblockClients(names);
    }

    private void actionDelete(List<ParcelableClientInfo> names) {
        mServerConnection.disconnectClients(names);
    }

    private void actionGetClients() {
 //       sendBroadCastClientsList(mServerConnection.getClientsInfo());
    }

    private void actionStatus() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, STATUS);
        if (mServerInfo != null && mServerInfo.isInit() && mServerConnection != null) {
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAME, mServerInfo);
            intent.putExtra(NAMES, mServerConnection.getClientsInfo());
        } else {
            intent.putExtra(IS_INIT, false);
        }

        sendBroadcast(intent);
    }

    private void actionClientsConnected() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, CLIENTS_CONNECTED);
        intent.putExtra(IS_CLIENTS_CONNECTED, mServerConnection.getClientsInfo().size() > 0);
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
        mJmDNSHelper.stop();
        sendBroadcast(intent);
    }

    @Override
    public void onClientsListChanged(ArrayList<ParcelableClientInfo> clientsInfo) {
        for (ParcelableClientInfo info : clientsInfo) {
            Log.d(TAG, info.toString());
        }

        actionStatus();
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

//    private void startBlockedThread() {
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                ArrayList<Connection> connections = mServerConnection.getAllBlockedConnections();
//                try {
//                    for (Connection connection : connections) {
//                        connection.sendMessage(JSONHelper.createBlockJson(mServerConnection.getId(), mWhiteList, mBlackList));
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 0, BLOCK_PERIOD);
//    }

    private void lockWifi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        lock.acquire();
    }

    @Override
    public void onDestroy() {
        Log.d("TEST", "on destroy");
        mJmDNSHelper.stop();
        super.onDestroy();
    }
}

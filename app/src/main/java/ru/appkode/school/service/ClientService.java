package ru.appkode.school.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.activity.BlockActivity;
import ru.appkode.school.activity.StudentActivity;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.ClientConnection;
import ru.appkode.school.network.FakeServer;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.Server;
import ru.appkode.school.util.JSONHelper;

/**
 * Created by lexer on 10.08.14.
 */
public class ClientService extends Service implements ClientConnection.OnStatusChanged, ClientConnection.OnServerInfoDownload {

    public static final int NOTIF_ID = 1;

    private static final String WHITE_LIST_PATH = "polymedia";
    private static final String WHITE_LIST_FILE_NAME = "whitelist";

    public static final String ACTION = "action";
    public static final String BROADCAST_ACTION = "ru.appkode.school.clientbroadcast";

    //Actions
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int IS_FREE = 3;
    public static final int CHANGE_NAME = 4;
    public static final int UPDATE_INFO = 5;
    public static final int CONNECT = 6;
    public static final int DISCONNECT = 7;
    public static final int GET_NAMES = 8;
    public static final int BLOCK = 9;
    public static final int UNBLOCK = 10;
    public static final int STATUS = 11;

    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String IS_INIT = "is_init";
    public static final String SHOW_BLOCK_DIALOG = "block_dialog";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;

    private ClientConnection mClientConnection;
    private FakeServer mFakeServer;

    private ArrayList<ParcelableServerInfo> mServersInfo;
    private List<NsdServiceInfo> mResolvedServers;
    private boolean mIsInfoSend = false;

    private ParcelableClientInfo mClientInfo;
    private List<String> whiteList;

    private boolean mIsFirstLaunch = true;

    private ScheduledExecutorService mScheduledExecutorService;
    private ActivityManager mActivityManager;
    private Future<?> mFuture;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TEST", "service onCreate");
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager, NsdName.CLIENT);
        mFakeServer = new FakeServer();
        mClientConnection = new ClientConnection();
        mClientConnection.setOnStatusChangedListener(this);
        mClientConnection.setOnServerInfoDownloadListener(this);
        mNsdName.start();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        lock.acquire();

        whiteList = getWhiteList();
        for (String wl : whiteList) {
            Log.d("WHITELIST", "white list test = " + wl);
        }

        this.startForeground();

        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mResolvedServers = new ArrayList<NsdServiceInfo>();

        mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mScheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();
    }

    private void startForeground() {
        startForeground(NOTIF_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsFirstLaunch) {
            if (checkSharedPreferences()) {
                Log.d("TEST", "start from shared pref");
                actionStart(mClientInfo);
            }
            mIsFirstLaunch = false;
        }
        if (intent != null) {
            int action = intent.getIntExtra(ACTION, -1);
            Log.d("TEST", "action in start command " + action);
            runAction(action, intent);
        }

        return START_STICKY;
    }

    private void runAction(int action, Intent intent) {
        Log.d("TEST", "action = " + action);
        if (action <= UPDATE_INFO) {
            ParcelableClientInfo clientInfo = intent.getParcelableExtra(NAME);
            switch (action) {
                case START:
                    actionStart(clientInfo);
                    break;
                case STOP:
                    actionStop();
                    break;
                case IS_FREE:
                    actionIsFree(clientInfo);
                    break;
                case UPDATE_INFO:
                    actionUpdateClientInfo(clientInfo);
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
        } else if (action == GET_NAMES) {
            actionGetNames();
        } else if (action == STATUS) {
            actionStatus();
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
        if (mClientConnection != null) {
            mClientConnection.disconnectFromServer(mClientInfo);
        }
        mClientInfo = clientInfo;
        writeSharedPreferences(mClientInfo);
        while (mFakeServer.getPort() == -1) {} // wait start server
        int port = mFakeServer.getPort();
        mNsdRegistration.setName(mClientInfo.clientId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();

        mClientConnection.setClientInfo(mClientInfo);

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, START);
        intent.putExtra(MESSAGE, "started");
        sendBroadcast(intent);
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
                boolean isNameFree = mNsdName.isNameFree(clientInfo.clientId);
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(CODE, IS_FREE);
                intent.putExtra(MESSAGE, isNameFree);
                sendBroadcast(intent);
            }
        }, 500);
    }

    private void actionUpdateClientInfo(ParcelableClientInfo clientInfo) {
        mClientConnection.setClientInfo(clientInfo);
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
        Log.d("TEST", "resolved size = " + mResolvedServers.size());
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
        Log.d("TEST", "clientInfo = " + (mClientInfo != null) + "serversinfo = " + (mServersInfo != null));
        if (mClientInfo != null && mServersInfo != null) {
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

    @Override
    public void OnServerInfoDownload(ParcelableServerInfo info) {
        mServersInfo.add(info);
        Log.d("TEST", "downloaded " + info.name + "  " + info.lastName);
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
    public void OnStatusChanged(final int status, final String serverId, boolean isNeedStatusRefresh) {
        ParcelableServerInfo info;
        switch (status) {
            case Server.BLOCK_CODE:
                mClientInfo.isBlocked = true;
                mClientInfo.blockedBy = serverId;
                info = getServerInfoById(serverId);
                info.isLocked = true;
                writeSharedPreferences(mClientInfo);
                updateNotification();
                block();
                actionStatus();
                break;
            case Server.UNBLOCK_CODE:
                mClientInfo.blockedBy = "none";
                mClientInfo.isBlocked = false;
                unblockAllServers();
                updateNotification();
                writeSharedPreferences(mClientInfo);
                unBlock();
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

    private boolean checkSharedPreferences() {
        Log.d("TEST", "check sp");
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
        String id = preferences.getString(Infos.ID, "nonesavedid");
        Log.d("TEST", "id = " + id);
        if (id.substring(0, 6).equals("client")) {
            String name = preferences.getString(Infos.CLIENT_NAME, "name");
            String lastName = preferences.getString(Infos.CLIENT_LASTNAME, "last_name");
            String group = preferences.getString(Infos.CLIENT_GROUP, "group");
            String blockBy = preferences.getString(Infos.CLIENT_BLOCKBY, "none");
            Log.d("TEST", "sp readed " + name + " " + lastName + " " + group + "  " + blockBy);
            if (name.equals("name")  || lastName.equals("last_name") || group.equals("group")) {
                return false;
            } else {
                Log.d("TEST", "create client info from sp");
                mClientInfo = new ParcelableClientInfo(name, lastName, group);
                mClientInfo.clientId = id;
                if (!blockBy.equals("none")) {
                    mClientInfo.isBlocked = true;
                    mClientInfo.blockedBy = blockBy;
                } else {
                    mClientInfo.isBlocked = false;
                }
                return true;
            }
        }

        return false;
    }

    private void writeSharedPreferences(ParcelableClientInfo info) {
        Log.d("TEST", "write sp " + info.name + " " + info.lastName);
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Infos.ID, info.clientId);
        editor.putString(Infos.CLIENT_NAME, info.name);
        editor.putString(Infos.CLIENT_LASTNAME, info.lastName);
        editor.putString(Infos.CLIENT_GROUP, info.group);
        editor.putString(Infos.CLIENT_BLOCKBY, info.blockedBy);
        editor.commit();

        Log.d("TEST", "sp writed");
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
                .build();
    }

    private List<String> getWhiteList() {

        List<String> whiteList;
        if (isExternalStorageAvailible()) {
            File path = Environment.getExternalStorageDirectory();
            path = new File(path.getAbsolutePath() + "/" + WHITE_LIST_PATH);
            File filePath = new File(path, WHITE_LIST_FILE_NAME);
            if (!path.exists()) {
                whiteList = readDefaultWhiteList();
            } else {
                whiteList = readWhiteList(filePath);
            }

            writeWhileList(whiteList, path, filePath);
        } else {
            whiteList = readDefaultWhiteList();
        }
        whiteList.add(getString(R.string.app_name));

        return whiteList;
    }

    private List<String> readDefaultWhiteList() {
        String[] defaultWhite = getResources().getStringArray(R.array.default_white_list);
        return new ArrayList<String>(Arrays.asList(defaultWhite));
    }

    private List<String> readWhiteList(File file) {
        List<String> whiteList = new ArrayList<String>();
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str = "";
            while ((str = br.readLine()) != null) {
                result += str;
            }
            Log.d("WHITELIST", "readed from file " + result);
        } catch (FileNotFoundException e) {
            Log.d("WHITELIST", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            whiteList = JSONHelper.parseWhiteList(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return whiteList;
    }

    private void writeWhileList(List<String> whiteList, File path, File filePath) {
        for (String s : whiteList) {
            if (s.equals(getString(R.string.app_name))) {
                whiteList.remove(s);
                break;
            }
        }
        String json = JSONHelper.createWhiteListJson(whiteList);
        path.mkdir();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            bw.write(json);
            bw.flush();
            bw.close();
        } catch (IOException e) {
        }
    }


    public boolean isExternalStorageAvailible() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private void block() {
        mFuture = mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                List<ActivityManager.RunningAppProcessInfo> appProcesses = mActivityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    ApkInfo apkInfo;
                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        apkInfo = getInfoFromPackageName(appProcess.pkgList[0], getApplicationContext());
                        if (isUserApp(apkInfo.info.applicationInfo)) {
                            boolean inWhiteList = false;
                            for (String app : whiteList) {
                                if (app.equals(apkInfo.appname)) {
                                    inWhiteList = true;
                                    break;
                                }
                            }
                            if (!inWhiteList) {
                                Intent lockIntent = new Intent(ClientService.this, BlockActivity.class);
                                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(lockIntent);
                            }
                        }
                    }
                }
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
    }

    public void unBlock() {
        if (mFuture != null) {
            mFuture.cancel(true);
        }
    }

    private boolean isUserApp(ApplicationInfo ai) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ai.flags & mask) == 0;
    }

    private static ApkInfo getInfoFromPackageName(String pkgName,
                                                  Context mContext) {
        ApkInfo newInfo = new ApkInfo();
        try {
            PackageInfo p = mContext.getPackageManager().getPackageInfo(
                    pkgName, PackageManager.GET_PERMISSIONS);

            newInfo.appname = p.applicationInfo.loadLabel(
                    mContext.getPackageManager()).toString();
            newInfo.pname = p.packageName;
            newInfo.versionName = p.versionName;
            newInfo.versionCode = p.versionCode;
            newInfo.icon = p.applicationInfo.loadIcon(mContext
                    .getPackageManager());
            newInfo.info = p;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return newInfo;
    }
    static class ApkInfo {

        ApkInfo() {
        }

        public String appname;
        public String pname;
        public String versionName;
        public int versionCode;
        public Drawable icon;
        public PackageInfo info;

    }

    @Override
    public void onDestroy() {
        Log.d("TEST", "onDestroy");
        mNsdRegistration.stop();
        mNsdName.stop();
        super.onDestroy();
    }
}

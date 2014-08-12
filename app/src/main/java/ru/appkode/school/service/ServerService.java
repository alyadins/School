package ru.appkode.school.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.Server;

/**
 * Created by lexer on 10.08.14.
 */
public class ServerService extends Service implements Server.OnClientListChanged {

    public static final String ACTION = "action";

    public static final String IS_RUN_PREF = "is_run_pref";

    //Actions
    public static final int START = 0;
    public static final int IS_NAME_FREE = 1;
    public static final int STOP = 2;
    public static final int BLOCK = 4;
    public static final int UNBLOCK = 5;
    public static final int DELETE = 6;
    public static final int GET_CLIENTS = 7;
    public static final int STATUS = 8;


    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";

    //Answers
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String IS_INIT = "is_init";

    public static final String BROADCAST_ACTION = "ru.appkode.school.serverbroadcast";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;

    private Server mServer;
    private ParcelableServerInfo mServerInfo;

    private boolean mIsRunning;
    private boolean mIsFirstLaunch = true;

    @Override
    public void onCreate() {
        Log.d("TEST", "on create");
        super.onCreate();
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager, NsdName.SERVER);
        mNsdName.start();
        mServer = new Server();
        mServer.start();
        mServer.setOnClientListChangedListener(this);
        mIsRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TEST", "on start mIsRunnging = " +mIsRunning);
        if (mIsFirstLaunch) {
            if (checkSharedPreferences()) {
                Log.d("TEST", "start from shared pref");
                actionStart(mServerInfo);
            }
            mIsFirstLaunch = false;
        }
        if (intent != null) {
            int command = intent.getIntExtra(ACTION, -1);
            Log.d("TEST", "action = " + command);
            runAction(command, intent);
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentText("content text");
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle("title");
        startForeground(1, builder.build());
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runAction(int action, Intent intent) {
        if (action <= STOP) {
            ParcelableServerInfo info = intent.getParcelableExtra(NAME);
            switch (action) {
                case START:
                    mServerInfo = info;
                    actionStart(mServerInfo);
                    break;
                case IS_NAME_FREE:
                    actionIsNameFree(info);
                    break;
                case STOP:
                    actionStop();
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
        } else if (action == GET_CLIENTS) {
            actionGetClients();
        } else if (action == STATUS) {
            actionStatus();
        }
    }

    //Actions methods
    private void actionStart(ParcelableServerInfo info) {
        Log.d("TEST", "on start is registered" + mNsdRegistration.isRegistered());
        if (!mNsdRegistration.isRegistered()) {
            startService(info);
        } else {
            mNsdRegistration.stop();
            while (mNsdRegistration.isRegistered()) {}
            startService(info);
        }
    }

    private void startService(ParcelableServerInfo info) {
        Log.d("TEST", "start service " + info.name + " " + info.lastName);
        writeSharedPreferences(info);
        mServer.setServerInfo(info);
        while (mServer.getPort() == -1) {}//Wait start server
        int port = mServer.getPort();

        mNsdRegistration.setName(info.serverId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();


        mIsRunning = true;
        sendSimpleBroadCast(START, "started");
    }

    private void actionIsNameFree(final ParcelableServerInfo info) {
        Log.d("TEST", "is name free");
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
                boolean isNameFree = mNsdName.isNameFree(info.serverId);
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(CODE, IS_NAME_FREE);
                intent.putExtra(MESSAGE, isNameFree);
                sendBroadcast(intent);
            }
        }, 500);
    }

    private void actionStop() {
        mNsdRegistration.stop();
        mIsRunning = false;
        sendSimpleBroadCast(STOP, "stop");
    }

    private void actionBlock(List<ParcelableClientInfo> names) {
        mServer.block(names);
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
        Log.d("TEST", "isRunning " + mIsRunning + " status");
    //    while (!mIsRunning) {};
        Log.d("TEST", "create status intent");
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, STATUS);
        if (mServerInfo != null && mServer != null) {
            intent.putExtra(IS_INIT, true);
            intent.putExtra(NAME, mServerInfo);
            intent.putExtra(NAMES, mServer.getClientsInfo());
        } else {
            intent.putExtra(IS_INIT, false);
        }
        Log.d("TEST", "send status");
        sendStickyBroadcast(intent);
    }

    @Override
    public void onClientListChanged(ArrayList<ParcelableClientInfo> clientsInfo) {
        sendBroadCastClientsList(clientsInfo);
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


    private boolean checkSharedPreferences() {
        Log.d("TEST", "check sp");
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
        String id = preferences.getString(Infos.ID, "none");
        if (id.substring(0, 4).equals("serv")) {
            String name = preferences.getString(Infos.SERVER_NAME, "name");
            String secondName = preferences.getString(Infos.SERVER_SECONDNAME, "second_name");
            String lastName = preferences.getString(Infos.SERVER_LASTNAME, "last_name");
            String subject = preferences.getString(Infos.SERVER_SUBJECT, "subject");
            Log.d("TEST", "sp readed " + name + " " + secondName + " " + lastName + " " + subject);
            if (name.equals("name") || secondName.equals("second_name") || lastName.equals("last_name") || subject.equals("subject")) {
                return false;
            } else {
                mServerInfo = new ParcelableServerInfo(lastName, name, secondName, subject);
                mServerInfo.serverId = id;
                return true;
            }
        }

        return false;
    }

    private void writeSharedPreferences(ParcelableServerInfo info) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Infos.ID, info.serverId);
        editor.putString(Infos.SERVER_NAME, info.name);
        editor.putString(Infos.SERVER_SECONDNAME, info.secondName);
        editor.putString(Infos.SERVER_LASTNAME, info.lastName);
        editor.putString(Infos.SERVER_SUBJECT, info.subject);
        editor.commit();

        Log.d("TEST", "sp writed");
    }

//    private boolean readRunningStatus() {
//        SharedPreferences preferences = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
//        boolean isRunning = preferences.getBoolean(IS_RUN_PREF, false);
//        Log.d("TEST", "read runnging status " + isRunning);
//
//        return isRunning;
//    }
//
//    private void writeRunningStatus() {
//        Log.d("TEST", "write running status " + mIsRunning);
//        SharedPreferences preferences = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
//        SharedPreferences.Editor editor = preferences.edit();
//        editor.putBoolean(IS_RUN_PREF, mIsRunning);
//        editor.commit();
//    }


    @Override
    public void onDestroy() {
        Log.d("TEST", "on destroy");
        mNsdName.stop();
        mNsdRegistration.stop();
        mIsRunning = false;
        super.onDestroy();
    }
}

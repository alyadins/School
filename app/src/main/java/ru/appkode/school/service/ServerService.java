package ru.appkode.school.service;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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

    //Actions
    public static final int START = 0;
    public static final int IS_NAME_FREE = 1;
    public static final int STOP = 2;
    public static final int BLOCK = 4;
    public static final int UNBLOCK = 5;
    public static final int DELETE = 6;
    public static final int GET_CLIENTS = 7;


    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";

    //Answers
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    public static final String BROADCAST_ACTION = "ru.appkode.school.serverbroadcast";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;

    private Server mServer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TEST", "service onCreate");
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager);
        mNsdName.start();
        mServer = new Server();
        mServer.start();
        mServer.setOnClientListChangedListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TEST", "onStartCommand");
        if (intent != null) {
            int command = intent.getIntExtra(ACTION, -1);
            runAction(command, intent);
        }
        return super.onStartCommand(intent, flags, startId);
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
                    actionStart(info);
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
        }
    }

    //Actions methods
    private void actionStart(ParcelableServerInfo info) {
        mServer.setServerInfo(info);
        while (mServer.getPort() == -1) {}//Wait start server
        int port = mServer.getPort();
        mNsdRegistration.setName(info.serverId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();

        sendSimpleBroadCast(START, "started");
    }

    private void actionIsNameFree(ParcelableServerInfo info) {
        if (!mNsdName.isDiscoveryStarted()) {
            throw new InternalError("NSD discovery isn't started");
        }

        boolean isNameFree = mNsdName.isNameFree(info.serverId);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, IS_NAME_FREE);
        intent.putExtra(MESSAGE, isNameFree);
        sendBroadcast(intent);
    }

    private void actionStop() {
        mNsdRegistration.stop();
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

    @Override
    public void onDestroy() {
        Log.d("TEST", "service onDestroy");
        super.onDestroy();
    }
}

package ru.appkode.school.service;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.ClientConnection;
import ru.appkode.school.network.FakeServer;
import ru.appkode.school.network.NsdName;
import ru.appkode.school.network.NsdRegistration;
import ru.appkode.school.network.Server;

/**
 * Created by lexer on 10.08.14.
 */
public class ClientService extends Service {

    public static final String ACTION = "action";
    public static final String BROADCAST_ACTION = "ru.appkode.school.clientbroadcast";
    //Actions
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int IS_FREE = 3;
    public static final int CONNECT = 4;
    public static final int DISCONNECT = 5;
    public static final int GET_NAMES = 6;
    public static final int BLOCK = 7;
    public static final int UNBLOCK = 8;

    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;

    private ClientConnection mClientConnection;
    private FakeServer mFakeServer;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TEST", "service onCreate");
        mManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdRegistration = new NsdRegistration(mManager);
        mNsdName = new NsdName(mManager);
        mFakeServer = new FakeServer();
        mClientConnection = new ClientConnection();
        mNsdName.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TEST", "onStartCommand");
        if (intent != null) {
            int action = intent.getIntExtra(ACTION, -1);
            Log.d("TEST", "action in start command " + action);
            runAction(action, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void runAction(int action, Intent intent) {
        Log.d("TEST", "action = " + action);
        if (action <= IS_FREE) {
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
            }
        } else if (action >= CONNECT && action <= DISCONNECT) {
            ParcelableServerInfo serverInfo = intent.getParcelableExtra(NAME);
            switch (action) {
                case CONNECT:
                    actionConnect(serverInfo);
                    break;
                case DISCONNECT:
                    actionDisconnect();
                    break;
            }
        } else if (action == GET_NAMES) {
            actionGetNames();
        }
    }

    private void actionStart(ParcelableClientInfo serverInfo) {
        while (mFakeServer.getPort() == -1) {} // wait start server
        int port = mFakeServer.getPort();
        mNsdRegistration.setName(serverInfo.clientId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, START);
        intent.putExtra(MESSAGE, "started");
        sendBroadcast(intent);
    }

    private void actionStop() {
        mNsdRegistration.stop();
    }

    private void actionIsFree(ParcelableClientInfo clientInfo) {
        while (!mNsdName.isDiscoveryStarted()) {}

        boolean isNameFree = mNsdName.isNameFree(clientInfo.clientId);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, IS_FREE);
        intent.putExtra(MESSAGE, isNameFree);
        sendBroadcast(intent);
    }

    private void actionConnect(ParcelableServerInfo serverInfo) {
        mClientConnection.connect(serverInfo);
    }

    private void actionDisconnect() {
        mClientConnection.disconnect();
    }

    private void actionGetNames() {
        mNsdName.resolveServers();
        while (!mNsdName.isResolveQueueEmpty()) {};
        List<NsdServiceInfo> resolvedServices = mNsdName.getResolvedServices();
        mClientConnection.askForServersInfo(resolvedServices);
        ArrayList<ParcelableServerInfo> infos = mClientConnection.getServersInfo();
        Log.d("TEST", "size = " + infos.size());
        for (ParcelableServerInfo i : infos) {
            Log.d("TEST", i.name + " " + i.lastName);
        }
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putParcelableArrayListExtra(NAMES, infos);
        intent.putExtra(CODE, GET_NAMES);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

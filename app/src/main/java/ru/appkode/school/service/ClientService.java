package ru.appkode.school.service;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
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
public class ClientService extends Service implements ClientConnection.OnStatusChanged, ClientConnection.OnServerInfoDownload {

    public static final String ACTION = "action";
    public static final String BROADCAST_ACTION = "ru.appkode.school.clientbroadcast";
    //Actions
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int IS_FREE = 3;
    public static final int UPDATE_INFO = 4;
    public static final int CONNECT = 5;
    public static final int DISCONNECT = 6;
    public static final int GET_NAMES = 7;
    public static final int BLOCK = 8;
    public static final int UNBLOCK = 9;
    public static final int STATUS = 10;

    //params
    public static final String NAME = "name";
    public static final String NAMES = "names";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";


    public static final String BLOCK_STATUS = "block_status";

    private NsdManager mManager;
    private NsdRegistration mNsdRegistration;
    private NsdName mNsdName;

    private ClientConnection mClientConnection;
    private FakeServer mFakeServer;

    private ArrayList<ParcelableServerInfo> mServersInfo;
    private List<NsdServiceInfo> mResolvedServers;
    private boolean mIsInfoSend = false;

    private boolean mIsBlock = false;

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

        mServersInfo = new ArrayList<ParcelableServerInfo>();
        mResolvedServers = new ArrayList<NsdServiceInfo>();
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
                    actionDisconnect();
                    break;
            }
        } else if (action == GET_NAMES) {
            actionGetNames();
        } else if (action == STATUS) {
            actionStatus();
        }
    }

    private void actionStart(ParcelableClientInfo clientInfo) {
        while (mFakeServer.getPort() == -1) {} // wait start server
        int port = mFakeServer.getPort();
        mNsdRegistration.setName(clientInfo.clientId);
        mNsdRegistration.setPort(port);
        mNsdRegistration.start();

        mClientConnection.setClientInfo(clientInfo);

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, START);
        intent.putExtra(MESSAGE, "started");
        sendBroadcast(intent);
    }

    private void actionStop() {
        mNsdRegistration.stop();
    }

    private void actionIsFree(ParcelableClientInfo clientInfo) {

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
        boolean isNameFree = mNsdName.isNameFree(clientInfo.clientId);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, IS_FREE);
        intent.putExtra(MESSAGE, isNameFree);
        sendBroadcast(intent);
    }

    private void actionUpdateClientInfo(ParcelableClientInfo clientInfo) {
        mClientConnection.setClientInfo(clientInfo);
    }


    private void actionConnect(ParcelableServerInfo serverInfo) {
        mClientConnection.connect(serverInfo);
    }


    private void actionDisconnect() {
        mClientConnection.disconnect();
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
        intent.putExtra(CODE, STATUS);
        intent.putExtra(BLOCK_STATUS, mIsBlock);
        intent.putExtra(NAMES, mServersInfo);
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
    public void OnStatusChanged(final int status, final String serverId) {
        switch (status) {
            case Server.BLOCK_CODE:
                mIsBlock = true;
                sendBroadCastMessage(BLOCK, serverId);
                break;
            case Server.UNBLOCK_CODE:
                mIsBlock = false;
                sendBroadCastMessage(UNBLOCK, null);
                break;
            case Server.CONNECTED:
                sendBroadCastMessage(CONNECT, serverId);
                break;
            case Server.DISCONNECTED:
                sendBroadCastMessage(DISCONNECT, serverId);
                break;
        }
    }

    private void sendBroadCastMessage(int action, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(CODE, action);
        if (message != null) {
            intent.putExtra(MESSAGE, message);
        }
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        mNsdRegistration.stop();
        mNsdName.stop();
        super.onDestroy();
    }
}

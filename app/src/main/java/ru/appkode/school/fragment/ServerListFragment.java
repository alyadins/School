package ru.appkode.school.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.gui.ServerListAdapter;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListFragment extends ListFragment implements  ServerListAdapter.OnClientStateChanged {

    public static final String TAG = "ServerListFragment";
    public static final int CONNECT = 0;
    public static final int DISCONNECT = 1;
    public static final int FAVOURITE = 2;

    private boolean mIsOnlyFavourite = false;

    private List<ParcelableServerInfo> mServerList;

    private ServerListAdapter mAdapter;

    private OnServerAction mOnServerAction;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServerList = new ArrayList<ParcelableServerInfo>();

        setServerList(mServerList);
    }

    public void setServerList(List<ParcelableServerInfo> serverList) {
        mServerList = serverList;
        if (serverList != null) {
            if (mAdapter == null) {
                mAdapter = new ServerListAdapter(getActivity(), R.layout.server_list_item, mServerList);
                mAdapter.setOnlyFavourite(mIsOnlyFavourite);
                mAdapter.setOnClientStateChangeListener(this);
                setListAdapter(mAdapter);
            } else {
                mAdapter.setOnlyFavourite(mIsOnlyFavourite);
                mAdapter.setData(serverList);
            }
        }
    }

    @Override
    public void onClientDisconnect(ParcelableServerInfo info) {
        if (mOnServerAction != null) {
            mOnServerAction.onServerAction(info, DISCONNECT);
        }
    }

    @Override
    public void onClientConnect(ParcelableServerInfo info) {
        if (mOnServerAction != null) {
            mOnServerAction.onServerAction(info, CONNECT);
        }
    }

    @Override
    public void onClientFavourite(ParcelableServerInfo info) {
        if (mOnServerAction != null) {
            mOnServerAction.onServerAction(info, FAVOURITE);
        }
    }

    public void setOnServerActionListener(OnServerAction l) {
        mOnServerAction = l;
    }

    public interface OnServerAction {
        public void onServerAction(ParcelableServerInfo info, int action);
    }


    public void setLocked(String serverId, boolean locked) {
        Log.d("TEST", "lock id = " + serverId + " locked = " + locked);
        for (ParcelableServerInfo info : mServerList) {
            if (info.id.equals(serverId)) {
                info.isLocked = locked;
                break;
            }
        }
        setServerList(mServerList);
    }

    public void setOnlyFavourite(boolean isOnlyFavourite) {
        mIsOnlyFavourite = isOnlyFavourite;
    }
}

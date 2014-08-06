package ru.appkode.school.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.gui.ServerListAdapter;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListFragment extends ListFragment implements  ServerListAdapter.OnClientStateChanged {

    public static final String TAG = "ServerListFragment";
    public static final int CONNECT = 0;
    public static final int DISCONNECT = 1;

    private List<ServerInfo> mServerList;

    private ServerListAdapter mAdapter;

    private OnServerAction mOnServerAction;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServerList = new ArrayList<ServerInfo>();

        setServerList(mServerList);
    }

    public void setServerList(List<ServerInfo> serverList) {
        mServerList = serverList;
        if (serverList != null) {
            if (mAdapter == null) {
                mAdapter = new ServerListAdapter(getActivity(), R.layout.server_list_item, mServerList);
                mAdapter.setOnClientStateChangeListener(this);
                setListAdapter(mAdapter);
            } else {
                mAdapter.setData(serverList);
            }
        }
    }

    @Override
    public void onClientDisconnect(ServerInfo info) {
        if (mOnServerAction != null) {
            mOnServerAction.onServerAction(info, DISCONNECT);
        }
    }

    @Override
    public void onClientConnect(ServerInfo info) {
        Log.d("TEST", "connect");
        if (mOnServerAction != null) {
            mOnServerAction.onServerAction(info, CONNECT);
        }
    }

    public void setOnServerActionListener(OnServerAction l) {
        mOnServerAction = l;
    }

    public interface OnServerAction {
        public void onServerAction(ServerInfo info, int action);
    }
}

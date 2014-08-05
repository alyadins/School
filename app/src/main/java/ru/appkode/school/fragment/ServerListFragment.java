package ru.appkode.school.fragment;

import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.appkode.school.R;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.gui.ServerListAdapter;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    public static final String TAG = "ServerListFragment";

    private List<ServerInfo> mServerList;

    private ServerListAdapter mAdapter;

    private OnServerChosen mOnServerChosen;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServerList = new ArrayList<ServerInfo>();

        setServerList(mServerList);
        getListView().setOnItemClickListener(this);
    }

    public void setServerList(List<ServerInfo> serverList) {
        mServerList = serverList;
        if (serverList != null) {
            if (mAdapter == null) {
                mAdapter = new ServerListAdapter(getActivity(), R.layout.server_list_item, mServerList);
                setListAdapter(mAdapter);
            } else {
                mAdapter.setData(serverList);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mOnServerChosen != null) {
            mOnServerChosen.onServerChosen(mServerList.get(position));
        }
    }

    public void setOnServerChosenListener(OnServerChosen l) {
        mOnServerChosen = l;
    }

    public interface OnServerChosen {
        public void onServerChosen(ServerInfo info);
    }
}

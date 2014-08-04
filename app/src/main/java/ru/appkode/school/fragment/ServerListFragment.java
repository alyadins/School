package ru.appkode.school.fragment;

import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.appkode.school.R;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.gui.ServerListAdapter;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListFragment extends ListFragment {

    public static final String TAG = "ServerListFragment";
    private List<ServerInfo> mServerList;

    private ServerListAdapter mAdapter;

    Random random = new Random();
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
                setListAdapter(mAdapter);
            } else {
                Log.d("TEST", "notify");
                mAdapter.setData(serverList);
            }
        }
    }

    private void generateServerList() {
        for (int i = 0; i < 10; i++) {
            String name = "name " + i;
            String subject = "subject " + i;
            boolean connected = random.nextBoolean();
            mServerList.add(new ServerInfo(name, subject, false, connected));
        }
    }
}

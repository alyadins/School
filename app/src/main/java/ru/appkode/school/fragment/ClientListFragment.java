package ru.appkode.school.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.Client;
import ru.appkode.school.gui.ClientListAdapter;
import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 02.08.14.
 */
public class ClientListFragment extends Fragment {

    public static final String TAG = "clientListFragment";

    private CheckBox mSelectAllCheckBox;
    private View mSelectAllContainer;
    private ListView mClientListView;

    private ClientListAdapter mAdapter;

    private List<Client> mClients;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_client_list, null);

        mSelectAllCheckBox = (CheckBox) v.findViewById(R.id.select_all_checkbox);
        mSelectAllContainer = v.findViewById(R.id.select_all);
        mClientListView = (ListView) v.findViewById(R.id.client_list);

        setClients(mClients);
        return v;
    }

    public void setClients(List<Client> clients) {
        mClients = clients;

        if (mClients == null)
            return;
        if (mAdapter == null)
            mAdapter = new ClientListAdapter(getActivity(), R.layout.fragment_client_list_item, mClients);

        mClientListView.setAdapter(mAdapter);

        mAdapter.notifyDataSetChanged();
    }
}

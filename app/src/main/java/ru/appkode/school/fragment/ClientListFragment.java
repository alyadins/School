package ru.appkode.school.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.gui.ClientListAdapter;

/**
 * Created by lexer on 02.08.14.
 */
public class ClientListFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static final String TAG = "clientListFragment";



    private CheckBox mSelectAllCheckBox;
    private View mSelectAllContainer;
    private ListView mClientListView;

    private ClientListAdapter mAdapter;

    private List<ParcelableClientInfo> mClientsInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_client_list, null);

        mSelectAllCheckBox = (CheckBox) v.findViewById(R.id.select_all_checkbox);
        mSelectAllContainer = v.findViewById(R.id.select_all);
        mClientListView = (ListView) v.findViewById(R.id.client_list);

        mSelectAllContainer.setOnClickListener(this);
        mSelectAllCheckBox.setOnCheckedChangeListener(this);

        setClients(mClientsInfo);
        return v;
    }

    public List<ParcelableClientInfo> getClientsInfo() {
        return mClientsInfo;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.select_all) {
            mSelectAllCheckBox.setChecked(!mSelectAllCheckBox.isChecked());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mSelectAllCheckBox) {
            for (ParcelableClientInfo info : mClientsInfo) {
                info.isChosen = isChecked;
                mAdapter.setClientsList(mClientsInfo);
            }
        }
    }

    public void setClients(List<ParcelableClientInfo> clientsInfo) {
        mClientsInfo = clientsInfo;

        if (mClientsInfo != null) {
            if (mAdapter == null) {
                mAdapter = new ClientListAdapter(getActivity(), R.layout.fragment_client_list_item, mClientsInfo);
                mClientListView.setAdapter(mAdapter);
            } else {
                mAdapter.setClientsList(mClientsInfo);
            }
        }
    }

}

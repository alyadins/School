package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ClientInfo;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.fragment.ServerListFragment;
import ru.appkode.school.fragment.StudentInfoFragment;
import ru.appkode.school.fragment.TabsFragment;
import ru.appkode.school.network.ClientConnection;
import ru.appkode.school.network.Server;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentActivity extends Activity implements ClientConnection.OnTeacherListChanged, ServerListFragment.OnServerAction, ClientConnection.OnStatusChanged {

    private ClientInfo mClientInfo;

    private StudentInfoFragment mStudentInfoFragment;
    private TabsFragment mTabsFragment;

    private FragmentManager mFragmentManager;

    private ClientConnection mClientConnection;

    private List<ServerInfo> mServersInfo;

    private ServerInfo mCurrentServer;

    private ServerListFragment mServerListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);
        showStudentLoginDialog();

        mFragmentManager = getFragmentManager();
        mStudentInfoFragment = (StudentInfoFragment) mFragmentManager.findFragmentByTag(StudentInfoFragment.TAG);

        if (mStudentInfoFragment == null) {
            mStudentInfoFragment = new StudentInfoFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.user_info, mStudentInfoFragment, StudentInfoFragment.TAG);
            transaction.commit();
        }

        mTabsFragment = (TabsFragment) mFragmentManager.findFragmentByTag(TabsFragment.TAG);
        if (mTabsFragment == null) {
            mTabsFragment = new TabsFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.server_info, mTabsFragment);
            transaction.commit();

            mTabsFragment.setLeftTitle(getString(R.string.favourites));
            mTabsFragment.setRightTitle(getString(R.string.server_list));

            mServerListFragment = (ServerListFragment) mFragmentManager.findFragmentByTag(ServerListFragment.TAG + "1");
            if (mServerListFragment == null) {
                mServerListFragment = new ServerListFragment();
                mServerListFragment.setOnServerActionListener(this);
                mTabsFragment.setRightFragment(mServerListFragment, ServerListFragment.TAG + "1");
            }
            ServerListFragment slf2 = new ServerListFragment();
            mTabsFragment.setLeftFragment(slf2, ServerListFragment.TAG + "2");
        }

        mClientConnection = new ClientConnection(this);
        mClientConnection.setOnTeacherListChangedListener(this);
        mClientConnection.setOnStatusChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClientConnection.discover();
        mServerListFragment.setServerList(mServersInfo);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClientConnection.stopDiscover();
    }

    @Override
    protected void onStop() {
        mClientConnection.disconnectFromServer(mClientInfo);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            Log.d("TEST", "onOption");
            mClientConnection.stopDiscover();
            mClientConnection.discover();
        } else
        if (item.getItemId() == R.id.about) {
            Log.d("TEST", "setting list with length = " + mServersInfo.size());
            mServerListFragment.setServerList(mClientConnection.getServersInfo());
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServerAction(ServerInfo info, int action) {
        switch (action) {
            case ServerListFragment.CONNECT:
                if (mClientInfo.connection == null)
                    mClientConnection.disconnectFromServer(mClientInfo);
                mClientConnection.connectToServer(info, mClientInfo);
                break;
            case ServerListFragment.DISCONNECT:
                mClientConnection.disconnectFromServer(mClientInfo);
        }
    }

    @Override
    public void onTeacherListChanged(ClientConnection connection, List<ServerInfo> serversInfo) {
        mServersInfo = serversInfo;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mServerListFragment.setServerList(mServersInfo);
            }
        });
    }

    private void showStudentLoginDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View v = inflater.inflate(R.layout.student_login_dialog, null);
        View titleView = inflater.inflate(R.layout.student_login_dialog_title, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(titleView)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadStudentInfo(v, dialog);
            }
        });
    }

    private void loadStudentInfo(View v, Dialog dialog) {
        String name = getTextFromEditTextById(R.id.name, v);
        String lastName = getTextFromEditTextById(R.id.last_name, v);
        String group = getTextFromEditTextById(R.id.group, v);


        if (checkForEmpty(this, name, R.string.error_enter_name) ||
                checkForEmpty(this, lastName, R.string.error_enter_last_name) ||
                checkForEmpty(this, group, R.string.error_enter_group))
            return;

        String clientId = "client" + StringUtil.md5(name + lastName + group);
        mClientInfo = new ClientInfo(name, lastName, group);
        mClientInfo.clientId = clientId;

        dialog.dismiss();

        setUserInfo();
    }

    @Override
    public void OnStatusChanged(final int status, final String serverId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ServerInfo info;
                switch (status) {
                    case Server.BLOCK_CODE:
                        mClientInfo.isBlocked = true;
                        mStudentInfoFragment.setBlock(true);
                        mClientInfo.blockedBy = serverId;
                        break;
                    case Server.UNBLOCK_CODE:
                        mClientInfo.isBlocked = false;
                        mStudentInfoFragment.setBlock(false);
                        break;
                    case Server.CONNECTED:
                        info = getServerInfoById(serverId);
                        if (info != null) {
                            info.isConnected = true;
                        }
                        mCurrentServer = info;
                        mServerListFragment.setServerList(mServersInfo);
                        break;
                    case Server.DISCONNECT:
                        info = getServerInfoById(serverId);
                        info.isConnected = false;
                        mServerListFragment.setServerList(mServersInfo);
                        break;
                }
            }
        });
    }

    private ServerInfo getServerInfoById(String serverId) {
        for (ServerInfo info : mServersInfo) {
            if (info.serverId.equals(serverId))
                return info;
        }

        return null;
    }

    private void setUserInfo() {

        mStudentInfoFragment.setUserName(mClientInfo.name + " " + mClientInfo.lastName);
        mStudentInfoFragment.setGroup(mClientInfo.group);
        mStudentInfoFragment.setBlock(mClientInfo.isBlocked);
    }
}

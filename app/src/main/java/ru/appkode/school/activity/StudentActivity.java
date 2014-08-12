package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.fragment.ServerListFragment;
import ru.appkode.school.fragment.StudentInfoFragment;
import ru.appkode.school.fragment.TabsFragment;
import ru.appkode.school.service.ClientService;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.service.ClientService.*;
import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentActivity extends Activity implements ServerListFragment.OnServerAction {

    //fragments
    private FragmentManager mFragmentManager;
    private StudentInfoFragment mStudentInfoFragment;
    private TabsFragment mTabsFragment;
    private ServerListFragment mServerListFragment;

    //data
    private List<ParcelableServerInfo> mServersInfo;
    private ParcelableServerInfo mCurrentServer;
    private ParcelableClientInfo mClientInfo;

    private AlertDialog mLoginDialog;
    private ProgressDialog mWaitDialog;
    private BroadcastReceiver mBroadcastReceiver;

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
        mServersInfo = new ArrayList<ParcelableServerInfo>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBroadCastReceiver();
        sendCommandToService(STATUS, null);
    }


    @Override
    protected void onPause() {
        unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
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
            sendCommandToService(GET_NAMES, null);
            mWaitDialog = new ProgressDialog(this);
            mWaitDialog.setMessage("Пожалуйста подождите");
            mWaitDialog.show();
        } else
        if (item.getItemId() == R.id.about) {
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServerAction(ParcelableServerInfo info, int action) {
        switch (action) {
            case ServerListFragment.CONNECT:
                sendConnectCommandToService(DISCONNECT, null);
                Handler handler = new Handler();
                sendConnectCommandToService(CONNECT, info);
                break;
            case ServerListFragment.DISCONNECT:
                sendConnectCommandToService(DISCONNECT, null);
        }
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

        mLoginDialog = builder.create();
        mLoginDialog.show();

        mLoginDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadStudentInfo(v);
            }
        });
    }

    private void loadStudentInfo(View v) {
        String name = getTextFromEditTextById(R.id.name, v);
        String lastName = getTextFromEditTextById(R.id.last_name, v);
        String group = getTextFromEditTextById(R.id.group, v);


        if (checkForEmpty(this, name, R.string.error_enter_name) ||
                checkForEmpty(this, lastName, R.string.error_enter_last_name) ||
                checkForEmpty(this, group, R.string.error_enter_group))
            return;

        String clientId = "client" + StringUtil.md5(name + lastName + group);
        mClientInfo = new ParcelableClientInfo(name, lastName, group);
        mClientInfo.clientId = clientId;

        Log.d("TEST", "send is free command");


        sendCommandToService(IS_FREE, mClientInfo);
    }


    private ParcelableServerInfo getServerInfoById(String serverId) {
        for (ParcelableServerInfo info : mServersInfo) {
            if (info.serverId.equals(serverId))
                return info;
        }

        return null;
    }

    private void initBroadCastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = intent.getIntExtra(CODE, -1);
                ParcelableServerInfo info;
                String id;
                Log.d("TEST", code + " ");
                switch (code) {
                    case START:
                        break;
                    case STOP:
                        break;
                    case IS_FREE:
                        boolean isFree = intent.getBooleanExtra(MESSAGE, false);
                        if (isFree) {
                            mLoginDialog.dismiss();
                            setUserInfo();
                        } else {
                            Toast.makeText(StudentActivity.this, "Занято", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case CONNECT:
                        id = intent.getStringExtra(MESSAGE);
                        if (id != null) {
                            info = getServerInfoById(id);
                            if (info != null && info.isConnected == false) {
                                if (mCurrentServer != null) {
                                    mCurrentServer.isConnected = false;
                                }
                                info.isConnected = true;
                                mCurrentServer = info;
                                mServerListFragment.setServerList(mServersInfo);
                            }
                        }
                        break;
                    case DISCONNECT:
                        id = intent.getStringExtra(MESSAGE);
                        info = getServerInfoById(id);
                        if (info != null) {
                            info.isConnected = false;
                            mServerListFragment.setServerList(mServersInfo);
                        }
                        break;
                    case GET_NAMES:
                        ArrayList<ParcelableServerInfo> servers = intent.getParcelableArrayListExtra(NAMES);
                        mServersInfo = servers;
                        mServerListFragment.setServerList(servers);
                        if (mWaitDialog != null)
                            mWaitDialog.dismiss();
                        break;
                    case BLOCK:
                        id = intent.getStringExtra(MESSAGE);
                        mClientInfo.isBlocked = true;
                        mStudentInfoFragment.setBlock(true);
                        mClientInfo.blockedBy = id;
                        sendCommandToService(UPDATE_INFO, mClientInfo);
                        break;
                    case UNBLOCK:
                        mClientInfo.isBlocked = false;
                        sendCommandToService(UPDATE_INFO, mClientInfo);
                        mStudentInfoFragment.setBlock(false);
                        break;
                    case STATUS:
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void sendCommandToService(int action, ParcelableClientInfo info) {
        Intent intent = new Intent(StudentActivity.this, ClientService.class);
        intent.putExtra(ACTION, action);
        if (info != null)
            intent.putExtra(NAME, info);
        startService(intent);
    }

    private void sendConnectCommandToService(int action, ParcelableServerInfo info) {
        Intent intent = new Intent(StudentActivity.this, ClientService.class);
        intent.putExtra(ACTION, action);
        if (info != null)
            intent.putExtra(NAME, info);
        startService(intent);
    }

    private void setUserInfo() {

        mStudentInfoFragment.setUserName(mClientInfo.name + " " + mClientInfo.lastName);
        mStudentInfoFragment.setGroup(mClientInfo.group);
        mStudentInfoFragment.setBlock(mClientInfo.isBlocked);

        sendCommandToService(START, mClientInfo);
    }
}

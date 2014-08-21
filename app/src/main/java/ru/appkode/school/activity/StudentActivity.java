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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.fragment.ServerListFragment;
import ru.appkode.school.fragment.StudentInfoFragment;
import ru.appkode.school.fragment.TabsFragment;
import ru.appkode.school.service.ClientService;
import ru.appkode.school.util.RegExpTestUtil;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.service.ClientService.*;
import static ru.appkode.school.service.ClientService.TAG;
import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentActivity extends Activity implements ServerListFragment.OnServerAction, StudentInfoFragment.OnShowApps {


    private static final String TAG = "StudentActivity";

    //fragments
    private FragmentManager mFragmentManager;
    private StudentInfoFragment mStudentInfoFragment;
    private TabsFragment mTabsFragment;
    private ServerListFragment mServerListFragment;
    private ServerListFragment mFavouriteListFragment;

    //data
    private ParcelableClientInfo mClientInfo;

    private AlertDialog mLoginDialog;
    private ProgressDialog mWaitDialog;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        mFragmentManager = getFragmentManager();
        mStudentInfoFragment = (StudentInfoFragment) mFragmentManager.findFragmentByTag(StudentInfoFragment.TAG);

        if (mStudentInfoFragment == null) {
            mStudentInfoFragment = new StudentInfoFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.user_info, mStudentInfoFragment, StudentInfoFragment.TAG);
            transaction.commit();
        }

        mStudentInfoFragment.setOnShowAppsListener(this);

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

            mFavouriteListFragment = (ServerListFragment) mFragmentManager.findFragmentByTag(ServerListFragment.TAG + "2");
            if (mFavouriteListFragment == null) {
                mFavouriteListFragment = new ServerListFragment();
                mFavouriteListFragment.setOnServerActionListener(this);
                mFavouriteListFragment.setOnlyFavourite(true);
                mTabsFragment.setLeftFragment(mFavouriteListFragment, ServerListFragment.TAG + "2");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBroadCastReceiver();
        sendSimpleCommandToService(STATUS);
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
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                sendSimpleCommandToService(GET_NAMES);
                break;
            case R.id.about:
                break;
            case R.id.change_username:
                sendSimpleCommandToService(CHANGE_NAME);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServerAction(ParcelableServerInfo info, int action) {
        switch (action) {
            case ServerListFragment.CONNECT:
                sendCommandToService(CONNECT, info);
                break;
            case ServerListFragment.DISCONNECT:
                if (mClientInfo.isBlocked) {
                    if (mClientInfo.blockedBy.equals(info.id)) {
                        return;
                    }
                }
                sendCommandToService(DISCONNECT, info);
                break;
            case ServerListFragment.FAVOURITE:
                sendCommandToService(FAVOURITE, info);
                break;
        }
    }

    private void showStudentLoginDialog(ParcelableClientInfo info) {
        LayoutInflater inflater = getLayoutInflater();
        final View v = inflater.inflate(R.layout.student_login_dialog, null);
        View titleView = inflater.inflate(R.layout.student_login_dialog_title, null);

        if (info != null) {
            ((EditText) v.findViewById(R.id.name)).setText(info.name);
            ((EditText) v.findViewById(R.id.last_name)).setText(info.lastName);
            ((EditText) v.findViewById(R.id.group)).setText(info.group);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(titleView)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        mLoginDialog = builder.create();
        Log.d("TEST", "show");
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
        if (!RegExpTestUtil.check(name, RegExpTestUtil.NAME) ||
                !RegExpTestUtil.check(lastName, RegExpTestUtil.NAME) ||
                !RegExpTestUtil.check(group, RegExpTestUtil.GROUP)) {
            Toast.makeText(this, R.string.error_incorrect_data, Toast.LENGTH_SHORT).show();
            return;
        }

        String clientId = "client" + StringUtil.md5((name + lastName + group).toLowerCase());
        boolean block = false;
        String blockBy = "none";
        if (mClientInfo != null) {
            block = mClientInfo.isBlocked;
            blockBy = mClientInfo.blockedBy;
        }
        mClientInfo = new ParcelableClientInfo(name, lastName, group);
        mClientInfo.id = clientId;
        mClientInfo.isBlocked = block;
        mClientInfo.blockedBy = blockBy;

        sendCommandToService(IS_FREE, mClientInfo);
    }


    private void initBroadCastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = intent.getIntExtra(CODE, -1);
                Log.d("TEST", "student activity code = " + code);
                switch (code) {
                    case START:
                        break;
                    case STOP:
                        break;
                    case IS_FREE:
                        boolean isFree = intent.getBooleanExtra(MESSAGE, false);
                        Log.d("TEST", "is name free = " + isFree);
                        if (isFree) {
                            mLoginDialog.dismiss();
                            sendCommandToService(START, mClientInfo);
                            setUserInfo();
                        } else {
                            Toast.makeText(StudentActivity.this, "Занято", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case CONNECT:
                        break;
                    case DISCONNECT:
                        break;
                    case GET_NAMES:
                        break;
                    case BLOCK:
                        break;
                    case UNBLOCK:
                        break;
                    case STATUS:
                        boolean isInit = intent.getBooleanExtra(IS_INIT, false);
                        Log.d(TAG, "is init = " + isInit);
                        if (isInit) {
                            mClientInfo = intent.getParcelableExtra(NAME);
                            ArrayList<ParcelableServerInfo> serverInfos = intent.getParcelableArrayListExtra(NAMES);
                            for (ParcelableServerInfo i : serverInfos) {
                                Log.d(TAG, i.toString());
                            }
                            setUserInfo();
                            Log.d(TAG, "status set");
                            mServerListFragment.setServerList(serverInfos);
                            mFavouriteListFragment.setServerList(serverInfos);
                            mStudentInfoFragment.setBlock(mClientInfo.isBlocked);
                        } else {
                            showStudentLoginDialog(null);
                        }
                        break;
                    case CHANGE_NAME:
                        boolean isNameInit = intent.getBooleanExtra(IS_INIT, false);
                        if (isNameInit) {
                            boolean isConnected = intent.getBooleanExtra(IS_CONNECTED, false);
                            if (!isConnected) {
                                ParcelableClientInfo info = intent.getParcelableExtra(NAME);
                                showStudentLoginDialog(info);
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(StudentActivity.this);
                                builder.setMessage(R.string.connected_to_server)
                                        .setTitle(R.string.error)
                                        .setPositiveButton(R.string.ok, null)
                                        .show();
                            }
                        } else {
                            showStudentLoginDialog(null);
                        }
                        break;
                    case WHITE_LIST:
                        boolean isWhiteListInit = intent.getBooleanExtra(IS_INIT, false);
                        if (isWhiteListInit) {
                            ArrayList<String> whiteList = intent.getStringArrayListExtra(WHITE_LIST_PARAM);
                            startAppsActivity(whiteList);
                        } else {
                            startAppsActivity(null);
                        }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }


    private void sendCommandToService(int action, ParcelableClientInfo info) {
        Intent intent = new Intent(StudentActivity.this, ClientService.class);
        intent.putExtra(ACTION, action);
        intent.putExtra(NAME, info);
        startService(intent);
    }

    private void sendSimpleCommandToService(int action) {
        Intent intent = new Intent(StudentActivity.this, ClientService.class);
        intent.putExtra(ACTION, action);
        startService(intent);
    }

    private void startAppsActivity(ArrayList<String> whiteList) {
        Intent appsIntent = new Intent(StudentActivity.this, AppActivity.class);
        appsIntent.putStringArrayListExtra(WHITE_LIST_PARAM, whiteList);
        startActivity(appsIntent);
    }

    private void sendCommandToService(int action, ParcelableServerInfo info) {
        Intent intent = new Intent(StudentActivity.this, ClientService.class);
        intent.putExtra(ACTION, action);
        intent.putExtra(NAME, info);
        startService(intent);
    }

    private void setUserInfo() {
        mStudentInfoFragment.setUserName(mClientInfo.name + " " + mClientInfo.lastName);
        mStudentInfoFragment.setGroup(mClientInfo.group);
        mStudentInfoFragment.setBlock(mClientInfo.isBlocked);
    }

    @Override
    public void onShowApps() {
        Log.d("TEST", "send white list command to service");
        sendSimpleCommandToService(WHITE_LIST);
    }
}

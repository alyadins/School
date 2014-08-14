package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.fragment.ClientListFragment;
import ru.appkode.school.fragment.TeacherInfoFragment;
import ru.appkode.school.service.ServerService;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.fragment.TeacherInfoFragment.BLOCK;
import static ru.appkode.school.fragment.TeacherInfoFragment.DELETE;
import static ru.appkode.school.fragment.TeacherInfoFragment.OnUserActionPerform;
import static ru.appkode.school.fragment.TeacherInfoFragment.TAG;
import static ru.appkode.school.fragment.TeacherInfoFragment.UNBLOCK;
import static ru.appkode.school.service.ServerService.*;
import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;


/**
 * Created by lexer on 01.08.14.
 */
public class TeacherActivity extends Activity implements OnUserActionPerform{

    private ParcelableServerInfo mServerInfo;

    private FragmentManager mFragmentManager;
    private TeacherInfoFragment mTeacherInfoFragment;
    private ClientListFragment mClientListFragment;

    private AlertDialog mDialog;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        intent.putExtra(ACTION, STATUS);
        startService(intent);

        mFragmentManager = getFragmentManager();

        mTeacherInfoFragment = (TeacherInfoFragment) mFragmentManager.findFragmentByTag(TAG);
        if (mTeacherInfoFragment == null) {
            mTeacherInfoFragment = new TeacherInfoFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.teacher_info, mTeacherInfoFragment, TAG);
            transaction.commit();
        }

        mTeacherInfoFragment.setOnUserActionPerformListener(this);

        mClientListFragment = (ClientListFragment) mFragmentManager.findFragmentByTag(ClientListFragment.TAG);
        if (mClientListFragment == null) {
            mClientListFragment = new ClientListFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.client_list, mClientListFragment, ClientListFragment.TAG);
            transaction.commit();
        }
    }

    @Override
    protected void onResume() {
        initBroadCastReceiver();
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mBroadcastReceiver);
        super.onPause();
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
        Intent intent;
        switch (id) {
            case R.id.refresh:
                intent = new Intent(TeacherActivity.this, ServerService.class);
                intent.putExtra(ACTION, GET_CLIENTS);
                startService(intent);
                break;
            case R.id.change_username:
                intent = new Intent(TeacherActivity.this, ServerService.class);
                intent.putExtra(ACTION, STOP);
                startService(intent);
                showTeacherLoginDialog();
                break;
            case R.id.about:
                Log.d("TEST", "clear sp");
                SharedPreferences preferences = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showTeacherLoginDialog() {
        final View v = getLayoutInflater().inflate(R.layout.teacher_login_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_user_name)
                .setView(v)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        mDialog = builder.create();
        mDialog.show();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadTeacherData(v);
            }
        });
    }


    private void loadTeacherData(View v) {
        String lastName = getTextFromEditTextById(R.id.last_name, v);
        String name = getTextFromEditTextById(R.id.name, v);
        String secondName = getTextFromEditTextById(R.id.second_name, v);
        String subject = getTextFromEditTextById(R.id.subject, v);

        if (checkForEmpty(this, lastName, R.string.error_enter_last_name) ||
                checkForEmpty(this, name, R.string.error_enter_name) ||
                checkForEmpty(this, secondName, R.string.error_enter_second_name) ||
                checkForEmpty(this, subject, R.string.error_enter_subject))
            return;

        mServerInfo = new ParcelableServerInfo(lastName, name, secondName, subject);

        StringBuffer buffer = new StringBuffer();
        buffer.append(mServerInfo.name)
                .append(" ")
                .append(mServerInfo.secondName)
                .append(" ")
                .append(mServerInfo.lastName)
                .append(" ")
                .append(mServerInfo.subject);

        mServerInfo.serverId = "serv" + StringUtil.md5(buffer.toString());


        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        intent.putExtra(ACTION, IS_NAME_FREE);
        intent.putExtra(NAME, mServerInfo);
        startService(intent);
    }

    private void setTeacherInfo() {
        mTeacherInfoFragment.setName(mServerInfo.name,
                mServerInfo.secondName,
                mServerInfo.lastName);

        mTeacherInfoFragment.setSubject(mServerInfo.subject);
    }

    @Override
    public void onUserActionPerform(int action) {
        ArrayList<ParcelableClientInfo> selectedClients = new ArrayList<ParcelableClientInfo>();
        for (ParcelableClientInfo info : mClientListFragment.getClientsInfo()) {
            if (info.isChosen == true) {
                selectedClients.add(info);
            }
        }
        switch (action) {
            case BLOCK:
                sendActionListToService(ServerService.BLOCK, selectedClients);
                break;
            case UNBLOCK:
                sendActionListToService(ServerService.UNBLOCK, selectedClients);
                break;
            case DELETE:
                sendActionListToService(ServerService.DELETE, selectedClients);
                break;
        }
    }

    private void initBroadCastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = intent.getIntExtra(CODE, 0);
                switch (code) {
                    case START:
                        Log.d("TEST", "server started");
                        break;
                    case IS_NAME_FREE:
                        boolean isFree = intent.getBooleanExtra(MESSAGE, false);
                        if (isFree) {
                            mDialog.dismiss();
                            sendCommandToService(START, mServerInfo);
                            sendCommandToService(STATUS, null);
                        } else {
                            Toast.makeText(TeacherActivity.this, "Занято", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case GET_CLIENTS:
                        ArrayList<ParcelableClientInfo> infos = intent.getParcelableArrayListExtra(NAMES);
                        if (infos != null)
                            mClientListFragment.setClients(infos);
                        break;
                    case ServerService.BLOCK:
                        Log.d("TEST", "blocked");
                        break;
                    case ServerService.UNBLOCK:
                        Log.d("TEST", "unblocked");
                        break;
                    case ServerService.DELETE:
                        Log.d("TEST", "deleted");
                        break;
                    case STATUS:
                        Log.d("TEST", "status");
                        boolean isInit = intent.getBooleanExtra(IS_INIT, false);
                        if (isInit) {
                            mServerInfo = intent.getParcelableExtra(NAME);
                            ArrayList<ParcelableClientInfo> i = intent.getParcelableArrayListExtra(NAMES);
                            mClientListFragment.setClients(i);
                            setTeacherInfo();
                        } else {
                            showTeacherLoginDialog();
                        }
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }


    private void sendCommandToService(int action, ParcelableServerInfo name) {
        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        intent.putExtra(NAME, name);
        intent.putExtra(ACTION, action);
        startService(intent);
    }

    private void sendActionListToService(int action, ArrayList<ParcelableClientInfo> infos) {
        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        intent.putExtra(ACTION, action);
        intent.putParcelableArrayListExtra(NAMES, infos);
        startService(intent);
    }
}

package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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
import ru.appkode.school.fragment.ClientListFragment;
import ru.appkode.school.fragment.TeacherInfoFragment;
import ru.appkode.school.service.ServerService;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.fragment.TeacherInfoFragment.BLOCK;
import static ru.appkode.school.fragment.TeacherInfoFragment.DELETE;
import static ru.appkode.school.fragment.TeacherInfoFragment.OnUserActionPerform;
import static ru.appkode.school.fragment.TeacherInfoFragment.TAG;
import static ru.appkode.school.fragment.TeacherInfoFragment.UNBLOCK;
import static ru.appkode.school.service.ServerService.ACTION;
import static ru.appkode.school.service.ServerService.BROADCAST_ACTION;
import static ru.appkode.school.service.ServerService.CHANGE_NAME;
import static ru.appkode.school.service.ServerService.CLIENTS_CONNECTED;
import static ru.appkode.school.service.ServerService.CODE;
import static ru.appkode.school.service.ServerService.GET_CLIENTS;
import static ru.appkode.school.service.ServerService.IS_CLIENTS_CONNECTED;
import static ru.appkode.school.service.ServerService.IS_INIT;
import static ru.appkode.school.service.ServerService.IS_NAME_FREE;
import static ru.appkode.school.service.ServerService.MESSAGE;
import static ru.appkode.school.service.ServerService.NAME;
import static ru.appkode.school.service.ServerService.NAMES;
import static ru.appkode.school.service.ServerService.START;
import static ru.appkode.school.service.ServerService.STATUS;
import static ru.appkode.school.service.ServerService.STOP;
import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;


public class TeacherActivity extends Activity implements OnUserActionPerform{

    private ParcelableServerInfo mServerInfo;

    private FragmentManager mFragmentManager;
    private TeacherInfoFragment mTeacherInfoFragment;
    private ClientListFragment mClientListFragment;

    private AlertDialog mDialog;
    private ProgressDialog mWaitDialog;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

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
        super.onResume();
        initBroadCastReceiver();
        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        intent.putExtra(ACTION, STATUS);
        startService(intent);
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
                intent = new Intent(TeacherActivity.this, ServerService.class);
                intent.putExtra(ACTION, CLIENTS_CONNECTED);
                startService(intent);
                break;
            case R.id.about:
                intent = new Intent(TeacherActivity.this, ServerService.class);
                intent.putExtra(ACTION, STOP);
                startService(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showTeacherLoginDialog(ParcelableServerInfo info) {
        final View v = getLayoutInflater().inflate(R.layout.teacher_login_dialog, null);

        if (info != null) {
            ((EditText) v.findViewById(R.id.last_name)).setText(info.lastName);
            ((EditText) v.findViewById(R.id.name)).setText(info.name);
            ((EditText) v.findViewById(R.id.second_name)).setText(info.secondName);
            ((EditText) v.findViewById(R.id.subject)).setText(info.subject);
        }

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

        StringBuilder buffer = new StringBuilder();
        buffer.append(mServerInfo.name)
                .append(mServerInfo.secondName)
                .append(mServerInfo.lastName)
                .append(mServerInfo.subject);

        mServerInfo.id = "serv" + StringUtil.md5(buffer.toString().toLowerCase());

        mWaitDialog = new ProgressDialog(TeacherActivity.this);
        mWaitDialog.setMessage(getString(R.string.wait));
        mWaitDialog.show();

        Intent nameIntent = new Intent(TeacherActivity.this, ServerService.class);
        nameIntent.putExtra(ACTION, IS_NAME_FREE);
        nameIntent.putExtra(NAME, mServerInfo);
        startService(nameIntent);
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
            if (info.isChosen) {
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
                Log.d("sharedPreferences", "broadcast recieve = " + code + " context = " + context.getPackageName());
                switch (code) {
                    case START:
                        sendCommandToService(STATUS, null);
                        break;
                    case IS_NAME_FREE:
                        boolean isFree = intent.getBooleanExtra(MESSAGE, false);
                        if (isFree) {
                            mDialog.dismiss();
                            sendCommandToService(START, mServerInfo);
                        } else {
                            Toast.makeText(TeacherActivity.this, "Занято", Toast.LENGTH_LONG).show();
                        }
                        mWaitDialog.dismiss();
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
                        boolean isInit = intent.getBooleanExtra(IS_INIT, false);
                        Log.d("sharedPreferences", "teacher activity status is init = " + isInit);
                        if (isInit) {
                            mServerInfo = intent.getParcelableExtra(NAME);
                            Log.d("sharedPreferences" , "teacher activity status = " + mServerInfo.name +" " + mServerInfo.lastName);
                            ArrayList<ParcelableClientInfo> i = intent.getParcelableArrayListExtra(NAMES);
                            mClientListFragment.setClients(i);
                            setTeacherInfo();
                        } else {
                            showTeacherLoginDialog(null);
                        }
                        break;
                    case CLIENTS_CONNECTED:
                        boolean isClientsConnected = intent.getBooleanExtra(IS_CLIENTS_CONNECTED, false);
                        if (isClientsConnected) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(TeacherActivity.this);
                            builder.setMessage(R.string.clients_connected)
                                    .setTitle(R.string.error)
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        } else {
                            sendCommandToService(CHANGE_NAME, null);
                        }
                        break;
                    case CHANGE_NAME:
                        boolean isNameInit = intent.getBooleanExtra(IS_INIT, false);
                        if (isNameInit) {
                            ParcelableServerInfo info = intent.getParcelableExtra(NAME);
                            showTeacherLoginDialog(info);
                        } else {
                            showTeacherLoginDialog(null);
                        }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }


    private void sendCommandToService(int action, ParcelableServerInfo name) {
        Intent intent = new Intent(TeacherActivity.this, ServerService.class);
        if (name != null) {
            intent.putExtra(NAME, name);
        }
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

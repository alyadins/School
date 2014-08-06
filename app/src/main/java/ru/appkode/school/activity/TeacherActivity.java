package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ClientInfo;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.fragment.ClientListFragment;
import ru.appkode.school.fragment.TeacherInfoFragment;
import ru.appkode.school.network.Server;
import ru.appkode.school.network.ServerNameChecker;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.fragment.TeacherInfoFragment.*;
import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;


/**
 * Created by lexer on 01.08.14.
 */
public class TeacherActivity extends Activity implements Server.OnClientListChanged, OnUserActionPerform, ServerNameChecker.NameChecked {

    private ServerInfo mServerInfo;

    private FragmentManager mFragmentManager;
    private TeacherInfoFragment mTeacherInfoFragment;
    private ClientListFragment mClientListFragment;

    private List<ClientInfo> mClientsInfo;

    private Server mServer;

    private String mServerName;

    private ServerNameChecker mNameChecker;

    private AlertDialog mDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        showTeacherLoginDialog();

        mFragmentManager = getFragmentManager();
        mClientsInfo = new ArrayList<ClientInfo>();

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
        mNameChecker = new ServerNameChecker(this);
        mNameChecker.setNameCheckedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mServer != null)
           mServer.registerService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mServer != null)
            mServer.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO add handlers
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onUserActionPerform(int action) {
        List<ClientInfo> selectedClients = new ArrayList<ClientInfo>();
        for (ClientInfo info : mClientsInfo) {
            if (info.isChosen == true) {
                selectedClients.add(info);
            }
        }
        switch (action) {
            case BLOCK:
                mServer.block(selectedClients);
                break;
            case UNBLOCK:
                mServer.unBlock(selectedClients);
                break;
            case DELETE:
                mServer.disconnect(selectedClients);
                break;
        }
    }

    @Override
    public void onNameChecked(final boolean isFree) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFree) {
                    mDialog.dismiss();
                    setTeacherInfo();
                } else {
                    Toast.makeText(TeacherActivity.this, "Занято", Toast.LENGTH_LONG).show();
                }
            }
        });
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

    @Override
    public void onClientListChanged(final List<ClientInfo> clientsInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mClientsInfo = clientsInfo;
                mClientListFragment.setClients(mClientsInfo);
            }
        });
    }


    private void startServer() {
        if (mServer == null) {
            mServer = new Server(this, mServerName);
            mServer.setServerInfo(mServerInfo);
            mServer.setOnNewUserConnectedListener(this);
        }
        mServer.start();
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

        mServerInfo = new ServerInfo(lastName, name, secondName, subject);

        StringBuffer buffer = new StringBuffer();
        buffer.append(mServerInfo.name)
                .append(" ")
                .append(mServerInfo.secondName)
                .append(" ")
                .append(mServerInfo.lastName)
                .append(" ")
                .append(mServerInfo.subject);

        mServerName = "serv" + StringUtil.md5(buffer.toString());
        mNameChecker.isNameFree(mServerName);
    }

    private void setTeacherInfo() {
        mTeacherInfoFragment.setName(mServerInfo.name,
                mServerInfo.secondName,
                mServerInfo.lastName);

        mTeacherInfoFragment.setSubject(mServerInfo.subject);

        mClientListFragment.setClients(mClientsInfo);
        mServerInfo.serverId = mServerName;

        startServer();
    }



}

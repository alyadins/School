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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.appkode.school.R;
import ru.appkode.school.data.Client;
import ru.appkode.school.data.TeacherInfo;
import ru.appkode.school.fragment.ClientListFragment;
import ru.appkode.school.fragment.TeacherInfoFragment;
import ru.appkode.school.network.NsdHelper;
import ru.appkode.school.network.RegistrationServer;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class TeacherActivity extends Activity {

    private TeacherInfo mTeacherInfo;

    private FragmentManager mFragmentManager;
    private TeacherInfoFragment mTeacherInfoFragment;
    private ClientListFragment mClientListFragment;

    private List<Client> mClients;

    private RegistrationServer mServer;
    private NsdHelper mNsdHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        showTeacherLoginDialog();

        mFragmentManager = getFragmentManager();
        mClients = new ArrayList<Client>();

        mTeacherInfoFragment = (TeacherInfoFragment) mFragmentManager.findFragmentByTag(TeacherInfoFragment.TAG);
        if (mTeacherInfoFragment == null) {
            mTeacherInfoFragment = new TeacherInfoFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.teacher_info, mTeacherInfoFragment, TeacherInfoFragment.TAG);
            transaction.commit();
        }

        mClientListFragment = (ClientListFragment) mFragmentManager.findFragmentByTag(ClientListFragment.TAG);
        if (mClientListFragment == null) {
            mClientListFragment = new ClientListFragment();
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.client_list, mClientListFragment, ClientListFragment.TAG);
            transaction.commit();
        }
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

    private void showTeacherLoginDialog() {
        final View v = getLayoutInflater().inflate(R.layout.teacher_login_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_user_name)
                .setView(v)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadTeacherData(v, dialog);
            }
        });
    }


    private void startServer() {
        if (mServer == null)
            mServer = new RegistrationServer();
        mServer.start();

        if (mNsdHelper == null)
            mNsdHelper = new NsdHelper(this);


        StringBuffer buffer = new StringBuffer();
        buffer.append(mTeacherInfo.name)
                .append(" ")
                .append(mTeacherInfo.secondName)
                .append(" ")
                .append(mTeacherInfo.lastName)
                .append(" ")
                .append(mTeacherInfo.subject);


        String serviceName = "serv" + StringUtil.md5(buffer.toString());
        mNsdHelper.setServiceName(serviceName);
        mNsdHelper.initializeNsd();
        if (mServer.getPort() > -1)
            mNsdHelper.registerService(mServer.getPort());
    }

    private void loadTeacherData(View v, Dialog dialog) {
        String lastName = getTextFromEditTextById(R.id.last_name, v);
        String name = getTextFromEditTextById(R.id.name, v);
        String secondName = getTextFromEditTextById(R.id.second_name, v);
        String subject = getTextFromEditTextById(R.id.subject, v);

        if (checkForEmpty(this, lastName, R.string.error_enter_last_name) ||
                checkForEmpty(this, name, R.string.error_enter_name) ||
                checkForEmpty(this, secondName, R.string.error_enter_second_name) ||
                checkForEmpty(this, subject, R.string.error_enter_subject))
            return;

        mTeacherInfo = new TeacherInfo(lastName, name, secondName, subject);

        dialog.dismiss();

        setTeacherInfo();

        startServer();
    }

    private void setTeacherInfo() {
        mTeacherInfoFragment.setName(mTeacherInfo.name,
                mTeacherInfo.secondName,
                mTeacherInfo.lastName);

        mTeacherInfoFragment.setSubject(mTeacherInfo.subject);

        generateFakeClients();

        mClientListFragment.setClients(mClients);
    }

    private void generateFakeClients() {
        Random random = new Random();
        for (int i = 0; i < 25; i++) {
            Client client = new Client();
            client.isLocked = random.nextBoolean();
            client.name = "name " + i;
            client.isLocketByOther = random.nextBoolean();
            client.group = i + "Б";
            client.isChosen = random.nextBoolean();
            mClients.add(client);
        }
    }

}

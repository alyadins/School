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
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.data.StudentInfo;
import ru.appkode.school.fragment.ServerListFragment;
import ru.appkode.school.fragment.StudentInfoFragment;
import ru.appkode.school.fragment.TabsFragment;
import ru.appkode.school.network.ClientConnection;

import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentActivity extends Activity implements ClientConnection.OnTeacherListChanged {

    private StudentInfo mStudentInfo;

    private StudentInfoFragment mStudentInfoFragment;
    private TabsFragment mTabsFragment;

    private FragmentManager mFragmentManager;

    private ClientConnection mClientConnection;

    private List<ServerInfo> mServersInfo;

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
                mTabsFragment.setRightFragment(mServerListFragment, ServerListFragment.TAG + "1");
            }
            ServerListFragment slf2 = new ServerListFragment();
            mTabsFragment.setLeftFragment(slf2, ServerListFragment.TAG + "2");
        }

        mClientConnection = new ClientConnection(this);
        mClientConnection.setOnTeacherListChangedListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mClientConnection.discover();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClientConnection.stopDiscover();
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
    public void onTeacherListChanged(ClientConnection connection, List<ServerInfo> serversInfo) {
        Log.d("TEST", "on teacher list changed");
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

        mStudentInfo = new StudentInfo(name, lastName, group);

        dialog.dismiss();

        setUserInfo();
    }

    private void setUserInfo() {

        mStudentInfoFragment.setUserName(mStudentInfo.name + " " + mStudentInfo.lastName);
        mStudentInfoFragment.setGroup(mStudentInfo.group);
        mStudentInfoFragment.setBlock(true);
    }
}

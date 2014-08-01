package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
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

import ru.appkode.school.R;
import ru.appkode.school.data.StudentInfo;
import ru.appkode.school.fragment.ServerListFragment;
import ru.appkode.school.fragment.TabsFragment;
import ru.appkode.school.fragment.UserInfoFragment;
import ru.appkode.school.util.StringUtil;

import static ru.appkode.school.util.StringUtil.checkForEmpty;
import static ru.appkode.school.util.StringUtil.getTextFromEditTextById;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentActivity extends Activity {

    private StudentInfo mStudentInfo;

    private UserInfoFragment mUserInfoFragment;
    private TabsFragment mTabsFragment;

    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);
        showStudentLoginDialog();

        if (mUserInfoFragment == null) {
            mUserInfoFragment = new UserInfoFragment();
            mFragmentManager = getFragmentManager();

            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.user_info, mUserInfoFragment, null);
            transaction.commit();
        }

        if (mTabsFragment == null) {
            mTabsFragment = new TabsFragment();
            mFragmentManager = getFragmentManager();

            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.server_info, mTabsFragment);
            transaction.commit();


            mTabsFragment.setLeftTitle(getString(R.string.favourites));
            mTabsFragment.setRightTitle(getString(R.string.server_list));
            ServerListFragment slf1 = new ServerListFragment();
            mTabsFragment.setLeftFragment(slf1);
            ServerListFragment slf2 = new ServerListFragment();
            mTabsFragment.setRightFragment(slf2);
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

        mUserInfoFragment.setUserName(mStudentInfo.name + " " + mStudentInfo.lastName);
        mUserInfoFragment.setGroup(mStudentInfo.group);
        mUserInfoFragment.setBlock(true);
    }
}

package ru.appkode.school.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.gui.AppListAdapter;
import ru.appkode.school.service.ClientService;
import ru.appkode.school.util.FileHelper;
import ru.appkode.school.util.JSONHelper;

/**
 * Created by lexer on 17.08.14.
 */
public class AppActivity extends Activity{

    private GridView mGridView;

    private ArrayList<String> mWhiteList;

    private ArrayList<AppInfo> appInfos;
    private AppListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_grid_view);
        mWhiteList = getIntent().getStringArrayListExtra(ClientService.WHITE_LIST_PARAM);

        appInfos = new ArrayList<AppInfo>();

        mGridView = (GridView) findViewById(R.id.grid_view);

        init();
    }

    private void init() {
        PackageManager pm = getPackageManager();

        List<ApplicationInfo> infos = getInstalledApplication();

        for (ApplicationInfo info : infos) {
            String label = (String) pm.getApplicationLabel(info);
            if (checkWhiteList(label)) {
                Drawable icon = info.loadIcon(pm);
                appInfos.add(new AppInfo(label, icon, info.packageName));
            }
        }

        addToGridView();
    }
    private List<ApplicationInfo> getInstalledApplication() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
        List<ApplicationInfo> appInfoList = new ArrayList();
        for (ApplicationInfo info : apps) {
            if (packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                appInfoList.add(info);
            }
        }
        Collections.sort(appInfoList, new ApplicationInfo.DisplayNameComparator(packageManager));
        return appInfoList;
    }


    private void addToGridView() {
        mAdapter = new AppListAdapter(this, R.layout.app_item, appInfos);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(appInfos.get(position));
            }
        });
    }

    private boolean checkWhiteList(String s) {
        if (mWhiteList == null) {
            return true;
        }
        for (String app : mWhiteList) {
            if (s.equals(app)) {
                return true;
            }
        }

        return false;
    }

    public static class AppInfo {

        public String appname;
        public Drawable icon;
        public String packageName;

        AppInfo(String appname, Drawable icon, String packageName) {
            this.appname = appname;
            this.icon = icon;
            this.packageName = packageName;
        }
    }

    private void launchApp(AppInfo info) {
        PackageManager manager = getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(info.packageName);
        if (intent != null){
            startActivity(intent);
        }
    }
}

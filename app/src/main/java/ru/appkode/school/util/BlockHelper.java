package ru.appkode.school.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.appkode.school.activity.BlockActivity;

/**
 * Created by lexer on 15.08.14.
 */
public class BlockHelper {

    private static final int CHECK_TIME = 300;

    private Future<?> mFuture;
    private ScheduledExecutorService mScheduledExecutorService;
    private ActivityManager mActivityManager;
    private Context mContext;
    private List<String> mWhiteList;
    private List<String> mBlackList;

    public BlockHelper(Context context) {
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mScheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();
    }

    public BlockHelper(Context context, List<String> whiteList, List<String> blackList) {
        this(context);
        mWhiteList = whiteList;
        mBlackList = blackList;
    }

    public void setWhiteList(List<String> list) {
        mWhiteList = list;
    }

    public void setBlackList(List<String> list) {
        mBlackList = list;
    }

    public void block() {
        mFuture = mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                List<ActivityManager.RunningAppProcessInfo> appProcesses = mActivityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    ApkInfo apkInfo;
                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        apkInfo = getInfoFromPackageName(appProcess.pkgList[0], mContext.getApplicationContext());
                        if (isUserApp(apkInfo.info.applicationInfo)) {
                            if (mWhiteList != null && !findInList(apkInfo.appname, mWhiteList)) {
                                sendLockIntent();
                            }
                        } else {
                            if (mBlackList != null && findInList(apkInfo.appname, mBlackList)) {
                                sendLockIntent();
                            }
                        }
                    }
                }
            }
        }, 0, CHECK_TIME, TimeUnit.MILLISECONDS);
    }

    private void sendLockIntent() {
        Intent lockIntent = new Intent(mContext, BlockActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(lockIntent);
    }

    private  boolean findInList(String appName,  List<String> list) {
        boolean contains = false;
        for (String name : list) {
            if (name.equals(appName)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    public void unBlock() {
        if (mFuture != null) {
            mFuture.cancel(true);
        }
    }

    private boolean isUserApp(ApplicationInfo ai) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ai.flags & mask) == 0;
    }

    private static ApkInfo getInfoFromPackageName(String pkgName,
                                                  Context mContext) {
        ApkInfo newInfo = new ApkInfo();
        try {
            PackageInfo p = mContext.getPackageManager().getPackageInfo(
                    pkgName, PackageManager.GET_PERMISSIONS);

            newInfo.appname = p.applicationInfo.loadLabel(
                    mContext.getPackageManager()).toString();
            newInfo.pname = p.packageName;
            newInfo.versionName = p.versionName;
            newInfo.versionCode = p.versionCode;
            newInfo.icon = p.applicationInfo.loadIcon(mContext
                    .getPackageManager());
            newInfo.info = p;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return newInfo;
    }
    static class ApkInfo {

        ApkInfo() {
        }

        public String appname;
        public String pname;
        public String versionName;
        public int versionCode;
        public Drawable icon;
        public PackageInfo info;

    }
}

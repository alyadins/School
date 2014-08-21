package ru.appkode.school.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.appkode.school.activity.BlockActivity;

/**
 * Created by lexer on 15.08.14.
 */
public class BlockHelper {

    private static final long CHECK_TIME = 100;

    public static final String IS_BLOCKED = "is_blocked";

    private ActivityManager mActivityManager;
    private Context mContext;
    private List<String> mWhiteList;
    private List<String> mBlackList;
    private Timer mTimer;
    private boolean mIsBlock = false;

    public BlockHelper(Context context) {
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
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
        if (!mIsBlock) {
            mIsBlock = true;
            mTimer = new Timer();
            for (String s : mWhiteList) {
                Log.d("WHITELIST ", s);
            }
            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    List<ActivityManager.RunningAppProcessInfo> appProcesses = mActivityManager.getRunningAppProcesses();
                    ApkInfo apkInfo;
                    for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            apkInfo = getInfoFromPackageName(appProcess.pkgList[0], mContext.getApplicationContext());
                            if (isUserApp(apkInfo.info.applicationInfo)) {
                                if (mWhiteList != null && !findInList(apkInfo.appname, mWhiteList)) {
                                    Log.d("SERVER_BLOCK", "send lock " + apkInfo.appname);
                                    sendLockIntent();
                                }
                            } else {
                                if (mBlackList != null && findInList(apkInfo.appname, mBlackList)) {
                                    sendLockIntent();
                                }
                            }
                        }
                    }
                    Log.d("SERVER_BLOCK", "finished");
                }
            }, 0, CHECK_TIME);
        }
    }

    private void sendLockIntent() {
        Log.d("TEST", "send lockintent");
        Intent lockIntent = new Intent(mContext, BlockActivity.class);
        lockIntent.putExtra(IS_BLOCKED, true);
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
        mIsBlock = false;
        if (mTimer != null)
            mTimer.cancel();
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

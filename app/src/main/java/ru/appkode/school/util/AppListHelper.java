package ru.appkode.school.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.appkode.school.R;

/**
 * Created by lexer on 15.08.14.
 */
public class AppListHelper extends FileHelper{
    public static final int BLACK_LIST = 0;
    public static final int WHITE_LIST = 1;

    private Context mContext;


    public AppListHelper(Context context) {
        mContext = context;
    }

    public List<String> getList(int type) {

        List<String> list = null;
        if (isExternalStorageAvailable()) {
            File path = getDirPath();
            File filePath = null;
            switch (type) {
                case WHITE_LIST:
                    filePath = getFilePath(path, WHITE_LIST_FILE_NAME);
                    if (!filePath.exists()) {
                        list = readDefaultWhiteList();
                    } else {
                        list = readList(filePath);
                    }
                    break;
                case BLACK_LIST:
                    filePath = getFilePath(path, BLACK_LIST_FILE_NAME);
                    if (!filePath.exists()) {
                        list = readDefaultBlackList();
                    } else {
                        list = readList(filePath);
                    }
                    break;
            }
            if (filePath != null && list != null) {
                writeList(list, path, filePath);
            }
        } else {
            switch (type) {
                case WHITE_LIST:
                    list = readDefaultWhiteList();
                    break;
                case BLACK_LIST:
                    list = readDefaultBlackList();
            }
        }
        if (type == WHITE_LIST) {
            list.add(mContext.getString(R.string.app_name));
        }

        return list;
    }


    private List<String> readDefaultWhiteList() {
        String[] defaultWhite = mContext.getResources().getStringArray(R.array.default_white_list);
        return new ArrayList<String>(Arrays.asList(defaultWhite));
    }

    private List<String> readDefaultBlackList() {
        String[] defaultBlack = mContext.getResources().getStringArray(R.array.default_black_list);
        return new ArrayList<String>(Arrays.asList(defaultBlack));
    }

    private List<String> readList(File file) {
        List<String> list = new ArrayList<String>();
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str;
            while ((str = br.readLine()) != null) {
                result += str;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            list = JSONHelper.parseAppList(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    private void writeList(List<String> list, File path, File filePath) {
        String json = JSONHelper.createAppListJson(list);
        path.mkdir();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            bw.write(json);
            bw.flush();
            bw.close();
        } catch (IOException e) {
        }
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

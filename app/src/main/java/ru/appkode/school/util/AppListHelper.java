package ru.appkode.school.util;

import android.content.Context;
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
public class AppListHelper {
    public static final int BLACK_LIST = 0;
    public static final int WHITE_LIST = 1;
    private static final String APP_LIST_PATH = "polymedia";
    private static final String WHITE_LIST_FILE_NAME = "whitelist";
    private static final String BLACK_LIST_FILE_NAME = "system_blacklist";

    private Context mContext;

    public AppListHelper(Context context) {
        mContext = context;
    }


    public List<String> getList(int type) {

        List<String> list = null;
        if (isExternalStorageAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            path = new File(path.getAbsolutePath() + "/" + APP_LIST_PATH);
            File filePath = null;
            switch (type) {
                case WHITE_LIST:
                    filePath = new File(path, WHITE_LIST_FILE_NAME);
                    if (!path.exists()) {
                        list = readDefaultWhiteList();
                    } else {
                        list = readList(filePath);
                    }
                    break;
                case BLACK_LIST:
                    filePath= new File(path, BLACK_LIST_FILE_NAME);
                    if (!path.exists()) {
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


    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}

package ru.appkode.school.util;

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
import java.util.List;

/**
 * Created by lexer on 19.08.14.
 */
public class FavouriteHelper extends FileHelper{
    private ArrayList<String> mFavouriteList;

    public FavouriteHelper() {
        init();
    }

    public void add(String serverId) {
        mFavouriteList.add(serverId);
    }

    public void remove(String serverId) {
        List<String> serversForRemove = new ArrayList<String>();
        for (String s : mFavouriteList) {
            if (s.equals(serverId)) {
                serversForRemove.add(s);
            }
        }

        mFavouriteList.removeAll(serversForRemove);
    }

    public void save() {
        writeFavouriteList();
    }

    public boolean isFavourite(String serverId) {
        for (String s : mFavouriteList) {
            if (s.equals(serverId)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
     //   mFavouriteList = readFavouriteList();
        if (mFavouriteList == null) {
            mFavouriteList = new ArrayList<String>();
        }
    }

    private ArrayList<String> readFavouriteList() {
        ArrayList<String> list = null;

        if (isExternalStorageAvailable()) {
            File path = getDirPath();
            File filePath = getFilePath(path, FAVOURITE);
            if (filePath.exists()) {
                String result = "";
                try {
                    BufferedReader br = new BufferedReader(new FileReader(filePath));
                    String str;
                    while ((str = br.readLine()) != null) {
                        result += str;
                    }
                    try {
                        Log.d("FAVOURITE", "readed" + result);
                        list = JSONHelper.parseFavouriteList(result);
                        for (String s : list) {
                            Log.d("FAVOURITE", "in fav list " + s);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return list;
    }

    private void writeFavouriteList() {
        if (isExternalStorageAvailable()) {
            String json = null;
            try {
                json = JSONHelper.createFavouriteJson(mFavouriteList);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getDirPath().mkdir();
            File path = getFilePath(getDirPath(), FAVOURITE);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(path));
                bw.write(json);
                bw.flush();
                bw.close();
            } catch (IOException e) {
            }
        }
    }
}

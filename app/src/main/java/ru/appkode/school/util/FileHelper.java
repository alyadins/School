package ru.appkode.school.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by lexer on 19.08.14.
 */
public abstract class FileHelper {
    public static final String APP_DIR = "polymedia";
    public static final String WHITE_LIST_FILE_NAME = "whitelist";
    public static final String BLACK_LIST_FILE_NAME = "system_blacklist";
    public static final String TEMP_WHITE_LIST = "tempWhiteList";
    public static final String FAVOURITE = "favourite";

    protected File getFilePath(File dirPath, String fileName) {
        File path = new File(dirPath, fileName);
        return path;
    }
    protected File getDirPath() {
        File path = Environment.getExternalStorageDirectory();
        path = new File(path.getAbsolutePath() + "/" + APP_DIR);

        return path;
    }

    protected boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}

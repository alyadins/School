package ru.appkode.school.util;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by lexer on 01.08.14.
 */
public class StringUtil {

    public static boolean checkForEmpty(Context context, String string, int errorMessageId) {
        if (string.isEmpty()) {
            Toast.makeText(context, errorMessageId, Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }

    public static String getTextFromEditTextById(int id, View v) {
        EditText editText = (EditText) v.findViewById(id);
        return editText.getText().toString();
    }
}

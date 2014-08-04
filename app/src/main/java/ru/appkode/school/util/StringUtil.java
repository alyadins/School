package ru.appkode.school.util;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static final String md5(final String s) {
        final String MD5 = "MD5";
        String result = null;
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            result = hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
}

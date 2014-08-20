package ru.appkode.school.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import ru.appkode.school.R;
import ru.appkode.school.util.BlockHelper;

/**
 * Created by lexer on 14.08.14.
 */
public class BlockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isLocked = getIntent().getBooleanExtra(BlockHelper.IS_BLOCKED, false);

        Log.d("TEST", "startBlockActivity is locked = " + isLocked);
        if (!isLocked) {
            finish();
        }

        showToast();

        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

        finish();
    }

    private void showToast() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.block_toast, null);

        final Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 200);
        toast.setView(v);
        toast.show();


        //for china tablets =))
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, 2500);

    }
}

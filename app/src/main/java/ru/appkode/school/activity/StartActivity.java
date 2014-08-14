package ru.appkode.school.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import ru.appkode.school.Infos;
import ru.appkode.school.R;
import ru.appkode.school.service.ClientService;
import ru.appkode.school.service.ServerService;
import ru.appkode.school.util.ServiceType;

import static ru.appkode.school.util.ServiceType.*;


public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    private void startClientActivity() {
        Intent stopIntent = new Intent(StartActivity.this, ServerService.class);
        stopService(stopIntent);
        Intent intent = new Intent(StartActivity.this, StudentActivity.class);
        startActivity(intent);
        finish();
    }

    private void startTeacherActivity() {
        Intent stopIntent = new Intent(StartActivity.this, ClientService.class);
        stopService(stopIntent);
        Intent intent = new Intent(StartActivity.this, TeacherActivity.class);
        startActivity(intent);
        finish();
    }

    public void startServer(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.server_warning)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startTeacherActivity();
                    }
                })
                .setNegativeButton(R.string.cancel,null)
                .show();
    }

    public void startClient(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.client_warning)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startClientActivity();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

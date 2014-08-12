package ru.appkode.school.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import ru.appkode.school.Infos;
import ru.appkode.school.util.ServiceType;

import static ru.appkode.school.util.ServiceType.*;

/**
 * Created by lexer on 12.08.14.
 */
public class StartService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences preferences = getSharedPreferences(Infos.PREFERENCES, MODE_PRIVATE);
        String id = preferences.getString(Infos.ID, "UNKNOWN_NAME");
        int type = getTypeOfService(id);
        Log.d("TEST", "startService runnging");
        switch (type) {
            case SERVER:
                startServerService();
                break;
            case CLIENT:
                startClientService();
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startServerService() {
        Log.d("TEST", "start server service");
        Intent intent = new Intent(getApplicationContext(), ServerService.class);
        getApplicationContext().startService(intent);
    }

    private void startClientService() {
        Log.d("TEST", "start client service");
        Intent intent = new Intent(getApplicationContext(), ClientService.class);
        getApplicationContext().startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

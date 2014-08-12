package ru.appkode.school;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.appkode.school.activity.StartActivity;
import ru.appkode.school.activity.TeacherActivity;
import ru.appkode.school.service.StartService;

/**
 * Created by lexer on 12.08.14.
 */
public class BootBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent startIntent = new Intent(context, StartService.class);
            context.startService(startIntent);
        }
    }
}

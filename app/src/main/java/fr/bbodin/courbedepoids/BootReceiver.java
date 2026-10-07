package fr.bbodin.courbedepoids;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        android.content.SharedPreferences p = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (p.getBoolean("reminder_enabled", false)) {
            ReminderScheduler.schedule(context, p.getInt("reminder_hour", 8), p.getInt("reminder_minute", 0));
        }
    }
}

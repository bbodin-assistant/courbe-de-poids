package fr.bbodin.courbedepoids;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "weight_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences p = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (p.getBoolean("reminder_enabled", false)) {
            ReminderScheduler.schedule(context, p.getInt("reminder_hour", 8), p.getInt("reminder_minute", 0));
        }

        createChannel(context);
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("focus_today", true);
        PendingIntent content = PendingIntent.getActivity(context, 4243, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle("Courbe de poids")
                .setContentText("Il est temps de noter votre poids du jour.")
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.notify(4244, notification);
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Rappel du poids",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Rappel quotidien pour enregistrer le poids");
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}

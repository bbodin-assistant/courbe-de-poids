package fr.bbodin.courbedepoids;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class AutoBackupScheduler {
    public static final String PREF_ENABLED = "automatic_backup_enabled";
    public static final String PREF_TREE_URI = "automatic_backup_tree_uri";
    public static final String PREF_LAST_SUCCESS = "automatic_backup_last_success";
    private static final String PERIODIC_WORK = "automatic-weight-backup-periodic";
    private static final String CHANGE_WORK = "automatic-weight-backup-on-change";

    private AutoBackupScheduler() {}

    public static void enable(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_ENABLED, false) || prefs.getString(PREF_TREE_URI, null) == null) return;
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                AutomaticBackupWorker.class, 24, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic);
        backupIfEnabled(context);
    }

    public static void backupIfEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_ENABLED, false) || prefs.getString(PREF_TREE_URI, null) == null) return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AutomaticBackupWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(CHANGE_WORK, ExistingWorkPolicy.REPLACE, request);
    }

    public static void disable(Context context) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelUniqueWork(PERIODIC_WORK);
        manager.cancelUniqueWork(CHANGE_WORK);
    }
}

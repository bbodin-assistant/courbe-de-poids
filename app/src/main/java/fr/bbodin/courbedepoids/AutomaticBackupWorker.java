package fr.bbodin.courbedepoids;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class AutomaticBackupWorker extends Worker {
    public AutomaticBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(AutoBackupScheduler.PREF_ENABLED, false)) return Result.success();
        String savedTree = prefs.getString(AutoBackupScheduler.PREF_TREE_URI, null);
        if (savedTree == null) return Result.failure();
        try {
            Uri tree = Uri.parse(savedTree);
            ContentResolver resolver = context.getContentResolver();
            Uri folder = DocumentsContract.buildDocumentUriUsingTree(tree,
                    DocumentsContract.getTreeDocumentId(tree));
            Uri target = null;
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                    DocumentsContract.getTreeDocumentId(tree));
            String[] columns = { DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME };
            try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
                if (cursor != null) while (cursor.moveToNext()) {
                    if ("courbe-de-poids-backup.json".equals(cursor.getString(1))) {
                        target = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0));
                        break;
                    }
                }
            }
            if (target == null) target = DocumentsContract.createDocument(resolver, folder,
                    "application/json", "courbe-de-poids-backup.json");
            if (target == null) return Result.retry();
            JSONObject json = BackupManager.createBackup(context, new WeightDatabase(context));
            try (OutputStream out = resolver.openOutputStream(target, "wt")) {
                if (out == null) return Result.retry();
                out.write((json.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            prefs.edit().putLong(AutoBackupScheduler.PREF_LAST_SUCCESS, System.currentTimeMillis()).apply();
            return Result.success();
        } catch (SecurityException e) {
            prefs.edit().putBoolean(AutoBackupScheduler.PREF_ENABLED, false).apply();
            AutoBackupScheduler.disable(context);
            return Result.failure();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}

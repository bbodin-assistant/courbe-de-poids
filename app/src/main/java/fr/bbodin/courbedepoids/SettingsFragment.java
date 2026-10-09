package fr.bbodin.courbedepoids;

import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

public class SettingsFragment extends Fragment {
    private static final int REQUEST_EXPORT = 2001;
    private static final int REQUEST_IMPORT = 2002;
    private static final int REQUEST_BACKUP_FOLDER = 2003;
    private Switch automaticBackup;
    private TextView backupLocation;
    private Switch enabled;
    private TextView time;
    private android.content.SharedPreferences prefs;
    private WeightDatabase db;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_settings, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(getActivity());
        prefs = getActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        populateAboutSection(view);
        enabled = view.findViewById(R.id.reminder_enabled);
        time = view.findViewById(R.id.reminder_time);
        enabled.setChecked(prefs.getBoolean("reminder_enabled", false));
        updateTimeText();
        time.setOnClickListener(v -> pickTime());
        view.findViewById(R.id.export_button).setOnClickListener(v -> exportData());
        view.findViewById(R.id.import_button).setOnClickListener(v -> importData());
        automaticBackup = view.findViewById(R.id.automatic_backup_enabled);
        backupLocation = view.findViewById(R.id.automatic_backup_location);
        updateBackupStatus();
        view.findViewById(R.id.choose_backup_folder).setOnClickListener(v -> chooseBackupFolder());
        automaticBackup.setChecked(prefs.getBoolean(AutoBackupScheduler.PREF_ENABLED, false));
        automaticBackup.setOnCheckedChangeListener((button, checked) -> {
            if (checked && prefs.getString(AutoBackupScheduler.PREF_TREE_URI, null) == null) {
                automaticBackup.setChecked(false);
                chooseBackupFolder();
                return;
            }
            prefs.edit().putBoolean(AutoBackupScheduler.PREF_ENABLED, checked).apply();
            if (checked) AutoBackupScheduler.enable(getActivity());
            else AutoBackupScheduler.disable(getActivity());
            updateBackupStatus();
        });
        enabled.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("reminder_enabled", checked).apply();
            if (checked) ReminderScheduler.schedule(getActivity(), prefs.getInt("reminder_hour", 8), prefs.getInt("reminder_minute", 0));
            else ReminderScheduler.cancel(getActivity());
            AutoBackupScheduler.backupIfEnabled(getActivity());
        });
    }

    private void populateAboutSection(View root) {
        TextView versionView = root.findViewById(R.id.app_version);
        TextView librariesView = root.findViewById(R.id.app_libraries);
        try {
            PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = info.versionName == null ? "Inconnue" : info.versionName;
            versionView.setText(version + " (" + info.versionCode + ")");
        } catch (PackageManager.NameNotFoundException e) { versionView.setText("Indisponible"); }
        librariesView.setText("• Android SDK (API minimale 26, cible 35)\n"
                + "• Java 17\n"
                + "• SQLite (stockage local)\n"
                + "• AndroidX Test Runner 1.7.0, Rules 1.7.0, JUnit 1.3.0 et Espresso 3.7.0 (tests uniquement)\n"
                + "Aucune bibliothèque tierce d’exécution : l’application utilise les API Android.");
    }

    private void updateTimeText() {
        time.setText(String.format(Locale.FRANCE, "Heure du rappel : %02d:%02d",
                prefs.getInt("reminder_hour", 8), prefs.getInt("reminder_minute", 0)));
    }

    private void pickTime() {
        new TimePickerDialog(getActivity(), (v, h, m) -> {
            prefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply();
            updateTimeText();
            if (enabled.isChecked()) ReminderScheduler.schedule(getActivity(), h, m);
            AutoBackupScheduler.backupIfEnabled(getActivity());
        }, prefs.getInt("reminder_hour", 8), prefs.getInt("reminder_minute", 0), true).show();
    }

    private void exportData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "courbe-de-poids.csv");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    public void startRestoreFlow() { importData(); }

    private void chooseBackupFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_BACKUP_FOLDER);
    }

    private void updateBackupStatus() {
        if (backupLocation == null) return;
        String uri = prefs.getString(AutoBackupScheduler.PREF_TREE_URI, null);
        boolean on = prefs.getBoolean(AutoBackupScheduler.PREF_ENABLED, false);
        if (!on) backupLocation.setText("Désactivée");
        else if (uri == null) backupLocation.setText("Choisissez un dossier pour démarrer.");
        else {
            long last = prefs.getLong(AutoBackupScheduler.PREF_LAST_SUCCESS, 0L);
            backupLocation.setText(last == 0L ? "Activée · première sauvegarde en attente"
                    : "Activée · dernière sauvegarde : " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(last)));
        }
    }

    private void importData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQUEST_BACKUP_FOLDER) {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getActivity().getContentResolver().takePersistableUriPermission(data.getData(), flags);
                prefs.edit().putString(AutoBackupScheduler.PREF_TREE_URI, data.getData().toString())
                        .putBoolean(AutoBackupScheduler.PREF_ENABLED, true).apply();
                automaticBackup.setOnCheckedChangeListener(null);
                automaticBackup.setChecked(true);
                automaticBackup.setOnCheckedChangeListener((button, checked) -> {
                    if (checked && prefs.getString(AutoBackupScheduler.PREF_TREE_URI, null) == null) {
                        automaticBackup.setChecked(false);
                        chooseBackupFolder();
                        return;
                    }
                    prefs.edit().putBoolean(AutoBackupScheduler.PREF_ENABLED, checked).apply();
                    if (checked) AutoBackupScheduler.enable(getActivity());
                    else AutoBackupScheduler.disable(getActivity());
                    updateBackupStatus();
                });
                AutoBackupScheduler.enable(getActivity());
                updateBackupStatus();
                Toast.makeText(getActivity(), "Sauvegarde automatique activée.", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_EXPORT) writeExport(data.getData());
            else if (requestCode == REQUEST_IMPORT) readImport(data.getData());
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Impossible de traiter le fichier.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeExport(Uri uri) throws Exception {
        try (OutputStream out = getActivity().getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("No output stream");
            out.write("date,weight_kg\n".getBytes(StandardCharsets.UTF_8));
            for (WeightDatabase.Measurement measurement : db.all()) {
                String row = measurement.date + "," + String.format(Locale.US, "%.2f", measurement.weight) + "\n";
                out.write(row.getBytes(StandardCharsets.UTF_8));
            }
        }
        Toast.makeText(getActivity(), "Fichier CSV téléchargé.", Toast.LENGTH_SHORT).show();
    }

    private void readImport(Uri uri) throws Exception {
        int imported = 0;
        try (InputStream in = getActivity().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("No input stream");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.toLowerCase(Locale.ROOT).startsWith("date,")) continue;
                String[] columns = line.split(",");
                if (columns.length < 2 || !columns[0].trim().matches("\\d{4}-\\d{2}-\\d{2}")) continue;
                try {
                    double weight = Double.parseDouble(columns[1].trim().replace(',', '.'));
                    if (!WeightDatabase.isValidWeight(weight)) continue;
                    db.save(columns[0].trim(), weight);
                    imported++;
                } catch (NumberFormatException ignored) { }
            }
        }
        Toast.makeText(getActivity(), imported + " mesure(s) importée(s) depuis le CSV.", Toast.LENGTH_LONG).show();
    }

}
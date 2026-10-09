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

public class SettingsFragment extends Fragment {
    private static final int REQUEST_EXPORT = 2001;
    private static final int REQUEST_IMPORT = 2002;
    private Switch enabled;
    private TextView time;
    private android.content.SharedPreferences prefs;
    private WeightDatabase db;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_settings, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(requireActivity());
        prefs = requireActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        populateAboutSection(view);
        enabled = view.findViewById(R.id.reminder_enabled);
        time = view.findViewById(R.id.reminder_time);
        enabled.setChecked(prefs.getBoolean("reminder_enabled", false));
        updateTimeText();
        time.setOnClickListener(v -> pickTime());
        view.findViewById(R.id.export_button).setOnClickListener(v -> exportData());
        view.findViewById(R.id.import_button).setOnClickListener(v -> importData());
        enabled.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("reminder_enabled", checked).apply();
            if (checked) ReminderScheduler.schedule(requireActivity(), prefs.getInt("reminder_hour", 8), prefs.getInt("reminder_minute", 0));
            else ReminderScheduler.cancel(requireActivity());
        });
    }

    private void populateAboutSection(View root) {
        TextView versionView = root.findViewById(R.id.app_version);
        TextView librariesView = root.findViewById(R.id.app_libraries);
        try {
            PackageInfo info = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
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
        new TimePickerDialog(requireActivity(), (v, h, m) -> {
            prefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply();
            updateTimeText();
            if (enabled.isChecked()) ReminderScheduler.schedule(requireActivity(), h, m);
        }, prefs.getInt("reminder_hour", 8), prefs.getInt("reminder_minute", 0), true).show();
    }

    private void exportData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "courbe-de-poids.csv");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void importData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQUEST_EXPORT) writeExport(data.getData());
            else if (requestCode == REQUEST_IMPORT) readImport(data.getData());
        } catch (Exception e) {
            Toast.makeText(requireActivity(), "Impossible de traiter le fichier.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeExport(Uri uri) throws Exception {
        try (OutputStream out = requireActivity().getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("No output stream");
            out.write("date,weight_kg\n".getBytes(StandardCharsets.UTF_8));
            for (WeightDatabase.Measurement m : db.all()) {
                out.write((m.date + "," + String.format(Locale.US, "%.2f", m.weight) + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        Toast.makeText(requireActivity(), "Export terminé.", Toast.LENGTH_SHORT).show();
    }

    private void readImport(Uri uri) throws Exception {
        int imported = 0;
        try (InputStream in = requireActivity().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("No input stream");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.toLowerCase(Locale.ROOT).startsWith("date,")) continue;
                String[] columns = line.split(",");
                if (columns.length < 2) continue;
                Double weight = parseWeight(columns[1]);
                if (columns[0].matches("\\d{4}-\\d{2}-\\d{2}") && WeightDatabase.isValidWeight(weight)) {
                    db.save(columns[0], weight);
                    imported++;
                }
            }
        }
        Toast.makeText(requireActivity(), imported + " mesure(s) importée(s).", Toast.LENGTH_SHORT).show();
    }

    private Double parseWeight(String value) {
        try { return Double.parseDouble(value.trim().replace(',', '.')); }
        catch (Exception e) { return null; }
    }
}
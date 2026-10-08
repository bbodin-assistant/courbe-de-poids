package fr.bbodin.courbedepoids;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS = "settings";
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private EditText weightInput;
    private WeightDatabase db;
    private WeightChartView chart;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new WeightDatabase(this);
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        weightInput = findViewById(R.id.weight_input);
        chart = findViewById(R.id.weight_chart);
        findViewById(R.id.save_today).setOnClickListener(v -> saveToday());
        findViewById(R.id.history_button).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.add_past_button).setOnClickListener(v -> startActivity(new Intent(this, AddMeasurementActivity.class)));
        findViewById(R.id.settings_button).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (db != null) refresh();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void refresh() {
        String today = today();
        WeightDatabase.Measurement m = db.get(today);
        weightInput.setText(m == null ? "" : format(m.weight));
        chart.setData(db.all());
        ((TextView)findViewById(R.id.today_status)).setText(
                m == null ? "Aucune mesure enregistrée aujourd'hui." : "Mesure du jour : " + format(m.weight) + " kg");
    }

    private void saveToday() {
        Double value = parse(weightInput.getText().toString());
        if (!WeightDatabase.isValidWeight(value)) {
            weightInput.setError("Entrez un poids valide.");
            return;
        }
        db.save(today(), value);
        Toast.makeText(this, "Poids enregistré.", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private Double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return null; }
    }

    private String format(double v) { return String.format(Locale.FRANCE, "%.1f", v); }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Les notifications sont désactivées. Les rappels ne pourront pas s'afficher.", Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(ReminderReceiver.CHANNEL_ID, "Rappel du poids", NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("Rappel quotidien pour enregistrer le poids");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
}

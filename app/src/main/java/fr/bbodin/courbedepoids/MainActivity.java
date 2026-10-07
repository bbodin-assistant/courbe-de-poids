package fr.bbodin.courbedepoids;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private EditText weightInput;
    private WeightDatabase db;
    private WeightChartView chart;
    private final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

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

    @Override protected void onResume() { super.onResume(); if (db != null) refresh(); }

    private void refresh() {
        WeightDatabase.Measurement m = db.get(today);
        weightInput.setText(m == null ? "" : format(m.weight));
        chart.setData(db.all());
        ((TextView)findViewById(R.id.today_status)).setText(
                m == null ? "Aucune mesure enregistrée aujourd'hui." : "Mesure du jour : " + format(m.weight) + " kg");
    }

    private void saveToday() {
        Double value = parse(weightInput.getText().toString());
        if (value == null || value <= 0) { weightInput.setError("Entrez un poids valide."); return; }
        db.save(today, value);
        Toast.makeText(this, "Poids enregistré.", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private Double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return null; }
    }
    private String format(double v) { return String.format(Locale.FRANCE, "%.1f", v); }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(ReminderReceiver.CHANNEL_ID, "Rappel du poids", NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("Rappel quotidien pour enregistrer le poids");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
}

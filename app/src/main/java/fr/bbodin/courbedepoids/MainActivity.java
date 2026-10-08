package fr.bbodin.courbedepoids;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS = "settings";
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private WeightDatabase db;
    private WeightChartView chart;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        setContentView(R.layout.activity_main);
        db = new WeightDatabase(this);
        chart = findViewById(R.id.weight_chart);

        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        findViewById(R.id.add_button).setOnClickListener(v ->
                startActivity(new Intent(this, AddMeasurementActivity.class)));
        findViewById(R.id.history_button).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.settings_button).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        seedDebugDataIfNeeded();
        refresh();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(15, 23, 42));
        window.setNavigationBarColor(Color.rgb(248, 250, 252));
        if (Build.VERSION.SDK_INT >= 26) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (db != null) refresh();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void seedDebugDataIfNeeded() {
        if (!BuildConfig.DEBUG || !db.all().isEmpty()) return;
        String[] dates = {"2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"};
        double[] weights = {82.4, 82.0, 81.7, 81.9, 81.5};
        for (int i = 0; i < dates.length; i++) db.save(dates[i], weights[i]);
    }

    private void refresh() {
        WeightDatabase.Measurement m = db.get(today());
        chart.setData(db.all());
        ((TextView) findViewById(R.id.today_status)).setText(
                m == null ? "Aucune mesure aujourd'hui" : "Aujourd'hui · " + format(m.weight) + " kg");
    }

    private String format(double v) {
        return String.format(Locale.FRANCE, "%.1f", v);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) {
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
            Toast.makeText(this, "Les notifications sont désactivées.", Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    ReminderReceiver.CHANNEL_ID, "Rappel du poids",
                    NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("Rappel quotidien pour enregistrer le poids");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
}

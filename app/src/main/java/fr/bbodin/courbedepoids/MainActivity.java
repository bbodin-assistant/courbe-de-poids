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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS = "settings";
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private WeightDatabase db;
    private WeightChartView chart;
    private TextView currentWeight, weightChange, periodCaption;
    private int selectedDays = 7;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        setContentView(R.layout.activity_main);
        db = new WeightDatabase(this);
        chart = findViewById(R.id.weight_chart);
        currentWeight = findViewById(R.id.current_weight);
        weightChange = findViewById(R.id.weight_change);
        periodCaption = findViewById(R.id.weight_period_caption);
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        BottomNavigation.bind(this);
        bindPeriod(R.id.period_7d, 7);
        bindPeriod(R.id.period_30d, 30);
        bindPeriod(R.id.period_3m, 90);
        bindPeriod(R.id.period_1y, 365);
        seedDebugDataIfNeeded();
        refresh();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(18, 107, 208));
        window.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 26) window.getDecorView().setSystemUiVisibility(
                window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private void bindPeriod(int id, int days) {
        findViewById(id).setOnClickListener(v -> { selectedDays = days; updatePeriodStyles(); refresh(); });
    }

    private void updatePeriodStyles() {
        int[] ids = {R.id.period_7d, R.id.period_30d, R.id.period_3m, R.id.period_1y};
        int[] days = {7, 30, 90, 365};
        for (int i = 0; i < ids.length; i++) {
            TextView button = findViewById(ids[i]);
            boolean active = selectedDays == days[i];
            button.setBackgroundResource(active ? R.drawable.bg_primary_button : R.drawable.bg_input);
            button.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#64748B"));
            button.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    @Override protected void onResume() { super.onResume(); if (db != null) refresh(); }

    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    private void seedDebugDataIfNeeded() {
        if (!BuildConfig.DEBUG || !db.all().isEmpty()) return;
        String[] dates = {"2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"};
        double[] weights = {82.4, 82.0, 81.7, 81.9, 81.5};
        for (int i = 0; i < dates.length; i++) db.save(dates[i], weights[i]);
    }

    private void refresh() {
        List<WeightDatabase.Measurement> all = db.all();
        if (all.isEmpty()) {
            currentWeight.setText("— kg");
            weightChange.setText("Aucune variation");
            weightChange.setTextColor(Color.GRAY);
            chart.setData(all);
            periodCaption.setText("Aucune mesure enregistrée");
            updatePeriodStyles();
            return;
        }
        WeightDatabase.Measurement latest = all.get(all.size() - 1);
        currentWeight.setText(format(latest.weight) + " kg");
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_YEAR, -(selectedDays - 1));
        String cutoffDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cutoff.getTime());
        List<WeightDatabase.Measurement> period = new ArrayList<>();
        for (WeightDatabase.Measurement m : all) if (m.date.compareTo(cutoffDate) >= 0) period.add(m);
        chart.setData(period);
        periodCaption.setText("Variation sur " + periodLabel());
        if (period.size() >= 2) {
            double delta = period.get(period.size() - 1).weight - period.get(0).weight;
            String symbol = delta > 0.0001 ? "↑ +" : delta < -0.0001 ? "↓ " : "→ ";
            weightChange.setText(symbol + String.format(Locale.FRANCE, "%.1f kg", delta));
            weightChange.setTextColor(delta > 0.0001 ? Color.rgb(220, 38, 38) :
                    delta < -0.0001 ? Color.rgb(22, 163, 74) : Color.rgb(100, 116, 139));
        } else {
            weightChange.setText("Pas assez de mesures");
            weightChange.setTextColor(Color.rgb(100, 116, 139));
        }
        updatePeriodStyles();
    }

    private String periodLabel() {
        if (selectedDays == 7) return "les 7 derniers jours";
        if (selectedDays == 30) return "les 30 derniers jours";
        if (selectedDays == 90) return "les 3 derniers mois";
        return "la dernière année";
    }

    private String format(double v) { return String.format(Locale.FRANCE, "%.1f", v); }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "Les notifications sont désactivées.", Toast.LENGTH_LONG).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(ReminderReceiver.CHANNEL_ID, "Rappel du poids", NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription("Rappel quotidien pour enregistrer le poids");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
}
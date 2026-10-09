package fr.bbodin.courbedepoids;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS = "settings";
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private int selectedTab = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        setContentView(R.layout.activity_main_host);
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        BottomNavigation.bind(this);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        } else {
            selectedTab = savedInstanceState.getInt("selected_tab", 0);
        }
        BottomNavigation.updateStyles(this, selectedTab);
        if (savedInstanceState == null && getIntent().getBooleanExtra("restore_backup_on_start", false)) {
            showTab(3);
            getFragmentManager().executePendingTransactions();
            android.app.Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof SettingsFragment) ((SettingsFragment) fragment).startRestoreFlow();
            getIntent().removeExtra("restore_backup_on_start");
        }
    }

    public void showTab(int tab) {
        if (tab < 0 || tab > 3) return;
        if (tab == selectedTab && getFragmentManager().findFragmentById(R.id.fragment_container) != null) return;
        selectedTab = tab;
        android.app.Fragment fragment;
        switch (tab) {
            case 1: fragment = new AddMeasurementFragment(); break;
            case 2: fragment = new HistoryFragment(); break;
            case 3: fragment = new SettingsFragment(); break;
            default: fragment = new HomeFragment(); break;
        }
        getFragmentManager().beginTransaction()
                .setTransition(android.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, fragment)
                .commit();
        BottomNavigation.updateStyles(this, selectedTab);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected_tab", selectedTab);
        super.onSaveInstanceState(outState);
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(20, 123, 239));
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
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
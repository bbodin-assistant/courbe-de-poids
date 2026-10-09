package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

/** Keeps the app's primary navigation available on every main screen. */
public final class BottomNavigation {
    private BottomNavigation() {}

    public static void bind(Activity activity) {
        bindTo(activity, R.id.home_button, MainActivity.class);
        bindTo(activity, R.id.add_button, AddMeasurementActivity.class);
        bindTo(activity, R.id.history_button, HistoryActivity.class);
        bindTo(activity, R.id.settings_button, SettingsActivity.class);
    }

    private static void bindTo(Activity activity, int id, Class<?> target) {
        View view = activity.findViewById(id);
        if (view != null) view.setOnClickListener(v -> {
            if (activity.getClass() == target) return;
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        });
    }
}
package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public final class BottomNavigation {
    private BottomNavigation() {}
    public static void bind(Activity activity) {
        bindTo(activity, R.id.home_button, MainActivity.class);
        bindTo(activity, R.id.add_button, AddMeasurementActivity.class);
        bindTo(activity, R.id.history_button, HistoryActivity.class);
        bindTo(activity, R.id.settings_button, SettingsActivity.class);
        int active = activity instanceof MainActivity ? R.id.home_button :
                activity instanceof AddMeasurementActivity ? R.id.add_button :
                activity instanceof HistoryActivity ? R.id.history_button : R.id.settings_button;
        int[] buttons = {R.id.home_button, R.id.add_button, R.id.history_button, R.id.settings_button};
        int[] labels = {R.id.nav_label_home, R.id.nav_label_add, R.id.nav_label_history, R.id.nav_label_settings};
        for (int i=0;i<buttons.length;i++) {
            boolean selected = buttons[i] == active;
            View v = activity.findViewById(buttons[i]);
            if (v instanceof ImageButton) ((ImageButton)v).setColorFilter(Color.parseColor(selected ? "#147BEF" : "#64748B"));
            TextView label = activity.findViewById(labels[i]);
            if (label != null) {
                label.setTextColor(Color.parseColor(selected ? "#147BEF" : "#64748B"));
                label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }
    private static void bindTo(Activity activity, int id, Class<?> target) {
        View view = activity.findViewById(id);
        if (view != null) view.setOnClickListener(v -> {
            if (activity.getClass() == target) return;
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            // Keep the bottom navigation visually fixed when switching screens.
            activity.overridePendingTransition(0, 0);
        });
    }
}
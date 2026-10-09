package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public final class BottomNavigation {
    private BottomNavigation() {}

    public static void bind(Activity activity) {
        int[] buttons = {R.id.home_button, R.id.add_button, R.id.history_button, R.id.settings_button};
        for (int i = 0; i < buttons.length; i++) {
            final int tab = i;
            View view = activity.findViewById(buttons[i]);
            if (view != null) view.setOnClickListener(v -> {
                if (activity instanceof MainActivity) ((MainActivity) activity).showTab(tab);
            });
        }
    }

    public static void updateStyles(Activity activity, int activeTab) {
        int[] buttons = {R.id.home_button, R.id.add_button, R.id.history_button, R.id.settings_button};
        int[] labels = {R.id.nav_label_home, R.id.nav_label_add, R.id.nav_label_history, R.id.nav_label_settings};
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = i == activeTab;
            View view = activity.findViewById(buttons[i]);
            if (view instanceof ImageButton) ((ImageButton) view).setColorFilter(Color.parseColor(selected ? "#147BEF" : "#64748B"));
            TextView label = activity.findViewById(labels[i]);
            if (label != null) {
                label.setTextColor(Color.parseColor(selected ? "#147BEF" : "#64748B"));
                label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }
}
package fr.bbodin.courbedepoids;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private LinearLayout list;
    private WeightDatabase db;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_history, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(requireActivity());
        list = view.findViewById(R.id.history_list);
        render();
    }

    @Override public void onResume() { super.onResume(); if (db != null && list != null) render(); }

    private void render() {
        list.removeAllViews();
        List<WeightDatabase.Measurement> measurements = db.all();
        for (WeightDatabase.Measurement m : measurements) {
            LinearLayout row = new LinearLayout(requireActivity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundResource(R.drawable.bg_card);
            row.setClickable(true);
            row.setOnClickListener(v -> edit(m));

            ImageView icon = new ImageView(requireActivity());
            icon.setImageResource(R.drawable.ic_calendar);
            icon.setContentDescription("Date");
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.setMarginEnd(dp(14));
            row.addView(icon, iconParams);

            LinearLayout info = new LinearLayout(requireActivity());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            String displayDate;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(m.date));
                displayDate = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(calendar.getTime());
            } catch (Exception e) { displayDate = m.date; }

            TextView date = new TextView(requireActivity());
            date.setText(displayDate); date.setTextSize(16); date.setTextColor(Color.rgb(15, 23, 42));
            date.setTypeface(null, android.graphics.Typeface.BOLD); info.addView(date);
            TextView weight = new TextView(requireActivity());
            weight.setText(String.format(Locale.FRANCE, "%.1f kg", m.weight));
            weight.setTextSize(15); weight.setTextColor(Color.rgb(71, 85, 105)); info.addView(weight);
            row.addView(info);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8); list.addView(row, rowParams);
            View divider = new View(requireActivity());
            divider.setBackgroundColor(Color.rgb(226, 232, 240));
            list.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        if (measurements.isEmpty()) {
            TextView empty = new TextView(requireActivity());
            empty.setText("Aucune mesure enregistrée."); empty.setTextSize(17);
            empty.setTextColor(Color.rgb(100, 116, 139)); empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, 0); list.addView(empty);
        }
    }

    private void edit(WeightDatabase.Measurement m) {
        final EditText input = new EditText(requireActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.FRANCE, "%.1f", m.weight));
        new AlertDialog.Builder(requireActivity()).setTitle("Modifier " + m.date).setView(input)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().replace(',', '.'));
                        if (!WeightDatabase.isValidWeight(value)) throw new IllegalArgumentException();
                        db.save(m.date, value); render();
                    } catch (Exception e) {
                        Toast.makeText(requireActivity(), "Poids invalide.", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Annuler", null).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
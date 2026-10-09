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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private LinearLayout list;
    private WeightDatabase db;

    private static class HistoryEntry {
        final String date;
        final long timestamp;
        final WeightDatabase.Measurement measurement;
        final WeightDatabase.WaterEvent water;
        HistoryEntry(WeightDatabase.Measurement measurement) {
            this.date = measurement.date; this.timestamp = 0; this.measurement = measurement; this.water = null;
        }
        HistoryEntry(WeightDatabase.WaterEvent water) {
            this.date = water.date; this.timestamp = water.createdAt; this.measurement = null; this.water = water;
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_history, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(getActivity());
        list = view.findViewById(R.id.history_list);
        render();
    }

    @Override public void onResume() { super.onResume(); if (db != null && list != null) render(); }

    private void render() {
        list.removeAllViews();
        List<HistoryEntry> entries = new ArrayList<>();
        for (WeightDatabase.Measurement m : db.all()) entries.add(new HistoryEntry(m));
        for (WeightDatabase.WaterEvent w : db.allWaterEvents()) entries.add(new HistoryEntry(w));
        Collections.sort(entries, new Comparator<HistoryEntry>() {
            @Override public int compare(HistoryEntry a, HistoryEntry b) {
                int byDate = a.date.compareTo(b.date);
                return byDate != 0 ? byDate : Long.compare(a.timestamp, b.timestamp);
            }
        });

        for (HistoryEntry entry : entries) {
            LinearLayout row = new LinearLayout(getActivity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundResource(R.drawable.bg_card);
            row.setClickable(entry.measurement != null || entry.water != null);
            if (entry.measurement != null) row.setOnClickListener(v -> edit(entry.measurement));
            else row.setOnClickListener(v -> editWater(entry.water));

            ImageView icon = new ImageView(getActivity());
            icon.setImageResource(entry.water == null ? R.drawable.ic_calendar : R.drawable.ic_water);
            icon.setContentDescription(entry.water == null ? "Poids" : "Eau");
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.setMarginEnd(dp(14));
            row.addView(icon, iconParams);

            LinearLayout info = new LinearLayout(getActivity());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            TextView date = new TextView(getActivity());
            date.setText(formatDate(entry.date)); date.setTextSize(16); date.setTextColor(Color.rgb(15, 23, 42));
            date.setTypeface(null, android.graphics.Typeface.BOLD); info.addView(date);
            TextView detail = new TextView(getActivity());
            if (entry.measurement != null) {
                detail.setText(String.format(Locale.FRANCE, "%.1f kg", entry.measurement.weight));
            } else {
                detail.setText("Eau · " + formatWater(entry.water.amountMl));
            }
            detail.setTextSize(15); detail.setTextColor(Color.rgb(71, 85, 105)); info.addView(detail);
            row.addView(info);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8); list.addView(row, rowParams);
        }
        if (entries.isEmpty()) {
            TextView empty = new TextView(getActivity());
            empty.setText("Aucune entrée enregistrée."); empty.setTextSize(17);
            empty.setTextColor(Color.rgb(100, 116, 139)); empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, 0); list.addView(empty);
        }
    }

    private void edit(WeightDatabase.Measurement m) {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.FRANCE, "%.1f", m.weight));
        new AlertDialog.Builder(getActivity()).setTitle("Modifier " + m.date).setView(input)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().replace(',', '.'));
                        if (!WeightDatabase.isValidWeight(value)) throw new IllegalArgumentException();
                        db.save(m.date, value); render();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Poids invalide.", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Annuler", null).show();
    }

    private void editWater(WeightDatabase.WaterEvent event) {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Quantité en ml");
        input.setText(String.valueOf(event.amountMl));
        new AlertDialog.Builder(getActivity()).setTitle("Modifier l’eau · " + event.date).setView(input)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    try {
                        int amount = Integer.parseInt(input.getText().toString().trim());
                        if (!WeightDatabase.isValidWaterAmount(amount)) throw new IllegalArgumentException();
                        db.updateWaterEvent(event.id, amount);
                        render();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Quantité invalide (1 à 10 000 ml).", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Annuler", null).show();
    }

    private String formatDate(String raw) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw));
            return new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(calendar.getTime());
        } catch (Exception e) { return raw; }
    }

    private String formatWater(int ml) {
        if (ml >= 1000 && ml % 1000 == 0) return (ml / 1000) + " L (" + ml + " ml)";
        return ml + " ml";
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

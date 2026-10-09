package fr.bbodin.courbedepoids;

import android.app.Fragment;
import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private WeightDatabase db;
    private WeightChartView chart;
    private TextView currentWeight, weightChange, periodCaption;
    private int selectedDays = 7;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(requireActivity());
        chart = view.findViewById(R.id.weight_chart);
        currentWeight = view.findViewById(R.id.current_weight);
        weightChange = view.findViewById(R.id.weight_change);
        periodCaption = view.findViewById(R.id.weight_period_caption);
        bindPeriod(view, R.id.period_7d, 7);
        bindPeriod(view, R.id.period_30d, 30);
        bindPeriod(view, R.id.period_3m, 90);
        bindPeriod(view, R.id.period_1y, 365);
        seedDebugDataIfNeeded();
        refresh();
    }

    @Override public void onResume() {
        super.onResume();
        if (db != null && getView() != null) refresh();
    }

    private void bindPeriod(View root, int id, int days) {
        root.findViewById(id).setOnClickListener(v -> { selectedDays = days; updatePeriodStyles(root); refresh(); });
    }

    private void updatePeriodStyles(View root) {
        int[] ids = {R.id.period_7d, R.id.period_30d, R.id.period_3m, R.id.period_1y};
        int[] days = {7, 30, 90, 365};
        for (int i = 0; i < ids.length; i++) {
            TextView button = root.findViewById(ids[i]);
            boolean active = selectedDays == days[i];
            button.setBackgroundResource(active ? R.drawable.bg_primary_button : R.drawable.bg_input);
            button.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#64748B"));
            button.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void seedDebugDataIfNeeded() {
        if (!BuildConfig.DEBUG || !db.all().isEmpty()) return;
        String[] dates = {"2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"};
        double[] weights = {82.4, 82.0, 81.7, 81.9, 81.5};
        for (int i = 0; i < dates.length; i++) db.save(dates[i], weights[i]);
    }

    private void refresh() {
        View root = getView();
        if (root == null || db == null) return;
        List<WeightDatabase.Measurement> all = db.all();
        if (all.isEmpty()) {
            currentWeight.setText("— kg");
            weightChange.setText("Aucune variation");
            weightChange.setTextColor(Color.GRAY);
            chart.setData(all);
            periodCaption.setText("Aucune mesure enregistrée");
            updatePeriodStyles(root);
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
        updatePeriodStyles(root);
    }

    private String periodLabel() {
        if (selectedDays == 7) return "les 7 derniers jours";
        if (selectedDays == 30) return "les 30 derniers jours";
        if (selectedDays == 90) return "les 3 derniers mois";
        return "la dernière année";
    }

    private String format(double value) { return String.format(Locale.FRANCE, "%.1f", value); }
}
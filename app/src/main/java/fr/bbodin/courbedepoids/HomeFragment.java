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
    private WaterChartView waterChart;
    private SportChartView sportChart;
    private UnifiedChartView overviewChart;
    private TextView currentWeight, weightChange, periodCaption;
    private int selectedDays = 7;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        db = new WeightDatabase(getActivity());
        chart = view.findViewById(R.id.weight_chart);
        waterChart = view.findViewById(R.id.water_chart);
        sportChart = view.findViewById(R.id.sport_chart);
        overviewChart = view.findViewById(R.id.overview_chart);
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
        if (!BuildConfig.DEBUG) return;
        List<WeightDatabase.Measurement> existingWeights = db.all();
        if (!db.allWaterEvents().isEmpty() || !db.allSportEvents().isEmpty()) return;
        boolean empty = existingWeights.isEmpty();
        boolean legacyDemo = existingWeights.size() == 5;
        if (legacyDemo) {
            String[] oldDates = {"2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"};
            double[] oldWeights = {82.4, 82.0, 81.7, 81.9, 81.5};
            for (int i = 0; i < oldDates.length; i++) {
                WeightDatabase.Measurement item = existingWeights.get(i);
                if (!oldDates[i].equals(item.date) || Math.abs(oldWeights[i] - item.weight) > 0.001) { legacyDemo = false; break; }
            }
        }
        if (!empty && !legacyDemo) return;

        java.util.Random random = new java.util.Random(20261009L);
        Calendar today = Calendar.getInstance(); clearTime(today);
        Calendar start = (Calendar) today.clone(); start.add(Calendar.DAY_OF_YEAR, -365);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        List<WeightDatabase.Measurement> weights = new ArrayList<>();
        Calendar weighDay = (Calendar) start.clone(); weighDay.add(Calendar.DAY_OF_YEAR, 2 + random.nextInt(4));
        int index = 0;
        while (!weighDay.after(today)) {
            double trend = 86.2 - (4.3 * index / 37.0);
            double value = Math.round((trend + (random.nextDouble() - 0.5) * 1.2) * 10.0) / 10.0;
            weights.add(new WeightDatabase.Measurement(fmt.format(weighDay.getTime()), value));
            index++;
            int gap = 5 + random.nextInt(8);
            if (random.nextInt(8) == 0) gap += 12 + random.nextInt(17);
            weighDay.add(Calendar.DAY_OF_YEAR, gap);
        }
        db.replaceAll(weights);

        List<WeightDatabase.WaterEvent> water = new ArrayList<>();
        Calendar day = (Calendar) start.clone();
        long waterId = 0, createdAt = start.getTimeInMillis();
        int skippedDays = 0;
        while (!day.after(today)) {
            if (skippedDays > 0) skippedDays--;
            else if (random.nextInt(100) < 78) {
                int amount = 1300 + random.nextInt(1501);
                water.add(new WeightDatabase.WaterEvent(-(++waterId), fmt.format(day.getTime()), amount, createdAt + waterId * 1000L));
            } else if (random.nextInt(10) == 0) skippedDays = 2 + random.nextInt(5);
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        db.replaceWaterEvents(water);

        List<WeightDatabase.SportEvent> sports = new ArrayList<>();
        Calendar sportDay = (Calendar) start.clone(); sportDay.add(Calendar.DAY_OF_YEAR, 1 + random.nextInt(5));
        long sportId = 0;
        while (!sportDay.after(today)) {
            if (random.nextInt(7) != 0) {
                String[] types = {"Yoga", "Course", "Escalade", "Workout"};
                String type = types[random.nextInt(types.length)];
                int duration = type.equals("Yoga") ? 25 + random.nextInt(46) : type.equals("Course") ? 20 + random.nextInt(51) : 30 + random.nextInt(61);
                double distance = type.equals("Course") ? Math.round((2.5 + random.nextDouble() * 8.0) * 10.0) / 10.0 : 0.0;
                sports.add(new WeightDatabase.SportEvent(-(++sportId), fmt.format(sportDay.getTime()), type, duration, distance, sportDay.getTimeInMillis() + sportId * 1000L));
            }
            int gap = 3 + random.nextInt(7);
            if (random.nextInt(6) == 0) gap += 10 + random.nextInt(20);
            sportDay.add(Calendar.DAY_OF_YEAR, gap);
        }
        db.replaceSportEvents(sports);
    }

    private void clearTime(Calendar value) {
        value.set(Calendar.HOUR_OF_DAY, 0); value.set(Calendar.MINUTE, 0);
        value.set(Calendar.SECOND, 0); value.set(Calendar.MILLISECOND, 0);
    }

    private void refresh() {
        View root = getView();
        if (root == null || db == null) return;
        List<WeightDatabase.Measurement> all = db.all();
        List<WeightDatabase.WaterEvent> waterEvents = db.allWaterEvents();
        List<WeightDatabase.SportEvent> sportEvents = db.allSportEvents();
        waterChart.setData(waterEvents, selectedDays);
        sportChart.setData(sportEvents, selectedDays);
        overviewChart.setData(all, waterEvents, sportEvents, selectedDays);
        updatePeriodStyles(root);
        if (all.isEmpty()) {
            currentWeight.setText("— kg");
            weightChange.setText("Aucune variation");
            weightChange.setTextColor(Color.GRAY);
            chart.setData(all);
            overviewChart.setData(all, db.allWaterEvents(), db.allSportEvents(), selectedDays);
            periodCaption.setText("Aucune mesure enregistrée");
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
    }

    private String periodLabel() {
        if (selectedDays == 7) return "les 7 derniers jours";
        if (selectedDays == 30) return "les 30 derniers jours";
        if (selectedDays == 90) return "les 3 derniers mois";
        return "la dernière année";
    }

    private String format(double value) { return String.format(Locale.FRANCE, "%.1f", value); }
}
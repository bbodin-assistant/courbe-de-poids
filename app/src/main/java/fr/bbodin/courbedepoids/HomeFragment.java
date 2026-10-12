package fr.bbodin.courbedepoids;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private WeightDatabase db;
    private GenericChartView chart;
    private GenericChartView waterChart;
    private GenericChartView sportChart;
    private GenericChartView overviewChart;
    private TextView currentWeight, weightChange, periodCaption;
    private int visibleDays = 7;
    private int endOffsetDays = 0;
    private final List<ChartRangeGestureListener> gestures = new ArrayList<>();
    private int minChartOffset = ChartRange.MIN_END_OFFSET_DAYS;
    private int maxChartOffset = ChartRange.MAX_END_OFFSET_DAYS;
    private int maxVisibleChartDays = 7;

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
        attachGestures(chart); attachGestures(overviewChart); attachGestures(waterChart); attachGestures(sportChart);
        safeRefresh();
    }

    @Override public void onResume() {
        super.onResume();
        if (db != null && getView() != null) safeRefresh();
    }

    private void bindPeriod(View root, int id, int days) {
        root.findViewById(id).setOnClickListener(v -> { visibleDays = days; endOffsetDays = 0; syncGestures(); updatePeriodStyles(root); safeRefresh(); });
    }

    private void updatePeriodStyles(View root) {
        int[] ids = {R.id.period_7d, R.id.period_30d, R.id.period_3m, R.id.period_1y};
        int[] days = {7, 30, 90, 365};
        for (int i = 0; i < ids.length; i++) {
            TextView button = root.findViewById(ids[i]);
            boolean active = visibleDays == days[i] && endOffsetDays == 0;
            button.setBackgroundResource(active ? R.drawable.bg_primary_button : R.drawable.bg_input);
            button.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#64748B"));
            button.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void attachGestures(View target) {
        ChartRangeGestureListener gesture = new ChartRangeGestureListener(target, visibleDays, endOffsetDays, (days, offset) -> { visibleDays = days; endOffsetDays = offset; syncGestures(); safeRefresh(); });
        gesture.setOffsetBounds(minChartOffset, maxChartOffset);
        gesture.setMaxVisibleDays(maxVisibleChartDays);
        gestures.add(gesture); target.setOnTouchListener(gesture);
    }
    private void syncGestures() { for (ChartRangeGestureListener gesture : gestures) { gesture.setOffsetBounds(minChartOffset, maxChartOffset); gesture.setMaxVisibleDays(maxVisibleChartDays); gesture.setRange(visibleDays, endOffsetDays); } }
    private void safeRefresh() {
        try {
            refresh();
        } catch (RuntimeException error) {
            android.util.Log.e("HomeFragment", "Unable to refresh home charts", error);
            if (currentWeight != null) currentWeight.setText("— kg");
            if (weightChange != null) weightChange.setText("Données temporairement indisponibles");
            if (periodCaption != null) periodCaption.setText("Impossible d’afficher les graphiques");
        }
    }

    private void refresh(){View root=getView();if(root==null||db==null)return;List<WeightDatabase.GenericEvent> events=db.allGenericEvents(),weights=new ArrayList<>();for(WeightDatabase.GenericEvent e:events)if("weight".equals(e.typeId))weights.add(e);java.util.Collections.sort(weights,(a,b)->a.date.compareTo(b.date));visibleDays=ChartRange.clampVisibleDays(visibleDays);
 if(!weights.isEmpty()){int first=ChartRange.dayNumber(weights.get(0).date),last=ChartRange.dayNumber(weights.get(weights.size()-1).date),today=ChartRange.dayNumber(ChartRange.format(Calendar.getInstance()));int forecast=Math.max(today,last)+ChartRange.PREDICTION_DAYS;int available=first==Integer.MIN_VALUE||last==Integer.MIN_VALUE||today==Integer.MIN_VALUE?7:Math.max(7,forecast-first+1);maxVisibleChartDays=ChartRange.clampVisibleDays(available);visibleDays=Math.min(visibleDays,maxVisibleChartDays);int[] bounds=ChartRange.allowedOffsets(Calendar.getInstance(),weights.get(0).date,weights.get(weights.size()-1).date,visibleDays);minChartOffset=bounds[0];maxChartOffset=bounds[1];endOffsetDays=ChartRange.clampOffsetToData(endOffsetDays,visibleDays,minChartOffset,maxChartOffset);}else{maxVisibleChartDays=7;minChartOffset=ChartRange.MIN_END_OFFSET_DAYS;maxChartOffset=ChartRange.MAX_END_OFFSET_DAYS;}
 endOffsetDays=ChartRange.clampEndOffsetDays(endOffsetDays);syncGestures();chart.setData(events,"weight","weight_kg","last",visibleDays,endOffsetDays);overviewChart.setData(events,"weight","weight_kg","last",visibleDays,endOffsetDays);waterChart.setData(events,"water","amount_ml","sum",visibleDays,endOffsetDays);sportChart.setData(events,"sport","duration_minutes","sum",visibleDays,endOffsetDays);updatePeriodStyles(root);
 if(weights.isEmpty()){currentWeight.setText("— kg");weightChange.setText("Aucune variation");weightChange.setTextColor(Color.GRAY);periodCaption.setText("Aucune mesure enregistrée");return;}WeightDatabase.GenericEvent latest=weights.get(weights.size()-1);currentWeight.setText(format(weightValue(latest))+" kg");ChartRange range=ChartRange.from(Calendar.getInstance(),visibleDays,endOffsetDays);List<WeightDatabase.GenericEvent> period=new ArrayList<>();for(WeightDatabase.GenericEvent e:weights)if(range.contains(e.date))period.add(e);periodCaption.setText(rangeLabel(range.start,range.end));
 if(period.size()>=2){double delta=weightValue(period.get(period.size()-1))-weightValue(period.get(0));String symbol=delta>0.0001?"↑ +":delta< -0.0001?"↓ ":"→ ";weightChange.setText(symbol+String.format(Locale.FRANCE,"%.1f kg",delta));weightChange.setTextColor(delta>0.0001?Color.rgb(220,38,38):delta< -0.0001?Color.rgb(22,163,74):Color.rgb(100,116,139));}else{weightChange.setText("Pas assez de mesures");weightChange.setTextColor(Color.rgb(100,116,139));}}
 private double weightValue(WeightDatabase.GenericEvent e){try{return new org.json.JSONObject(e.payload).getDouble("weight_kg");}catch(Exception x){return 0;}}
    private String rangeLabel(Calendar start, Calendar end) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        return "Du " + fmt.format(start.getTime()) + " au " + fmt.format(end.getTime());
    }

    private String format(double value) { return String.format(Locale.FRANCE, "%.1f", value); }
}
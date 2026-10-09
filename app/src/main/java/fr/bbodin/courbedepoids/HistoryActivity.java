package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private LinearLayout list;
    private WeightDatabase db;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_history);
        BottomNavigation.bind(this);
        db = new WeightDatabase(this);
        list = findViewById(R.id.history_list);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (db != null) render();
    }

    private void render() {
        list.removeAllViews();
        List<WeightDatabase.Measurement> ms = db.all();
        for (WeightDatabase.Measurement m : ms) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundResource(R.drawable.bg_card);
            row.setClickable(true);
            row.setOnClickListener(v -> edit(m));

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_calendar);
            icon.setContentDescription("Date");
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.setMarginEnd(dp(14));
            row.addView(icon, iconParams);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            String displayDate;
            try {
                DateParts p = parseDate(m.date);
                displayDate = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(p.calendar.getTime());
            } catch (Exception e) {
                displayDate = m.date;
            }
            TextView date = new TextView(this);
            date.setText(displayDate);
            date.setTextSize(16);
            date.setTextColor(Color.rgb(15, 23, 42));
            date.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(date);

            TextView weight = new TextView(this);
            weight.setText(String.format(Locale.FRANCE, "%.1f kg", m.weight));
            weight.setTextSize(15);
            weight.setTextColor(Color.rgb(71, 85, 105));
            info.addView(weight);
            row.addView(info);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            list.addView(row, rowParams);
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(226, 232, 240));
            list.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        if (ms.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucune mesure enregistrée.");
            empty.setTextSize(17);
            empty.setTextColor(Color.rgb(100, 116, 139));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, 0);
            list.addView(empty);
        }
    }

    private void edit(WeightDatabase.Measurement m) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.FRANCE, "%.1f", m.weight));
        new AlertDialog.Builder(this).setTitle("Modifier " + m.date).setView(input)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().replace(',', '.'));
                        if (!WeightDatabase.isValidWeight(value)) throw new IllegalArgumentException();
                        db.save(m.date, value);
                        render();
                    } catch (Exception e) {
                        Toast.makeText(this, "Poids invalide.", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Annuler", null).show();
    }

    private DateParts parseDate(String value) throws Exception {
        CalendarHolder holder = new CalendarHolder();
        java.util.Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value);
        holder.calendar.setTime(parsed);
        return new DateParts(holder.calendar);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class CalendarHolder {
        final java.util.Calendar calendar = java.util.Calendar.getInstance();
    }
    private static class DateParts {
        final java.util.Calendar calendar;
        DateParts(java.util.Calendar calendar) { this.calendar = calendar; }
    }
}
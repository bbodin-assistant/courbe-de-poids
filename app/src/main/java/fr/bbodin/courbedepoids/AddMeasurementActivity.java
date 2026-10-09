package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMeasurementActivity extends Activity {
    private TextView dateInput;
    private TextView todayHint;
    private EditText weightInput;
    private final Calendar date = Calendar.getInstance();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_add_measurement);
        BottomNavigation.bind(this);
        dateInput = findViewById(R.id.date_input);
        todayHint = findViewById(R.id.date_today_hint);
        weightInput = findViewById(R.id.weight_input);

        updateDate();
        dateInput.setOnClickListener(v -> openDatePicker());
        findViewById(R.id.save_measurement).setOnClickListener(v -> save());
    }

    private void openDatePicker() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            date.set(y, m, d);
            updateDate();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDate() {
        Calendar selected = (Calendar) date.clone();
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        String label;
        if (selected.getTimeInMillis() == today.getTimeInMillis()) {
            label = "Aujourd'hui";
        } else {
            Calendar yesterday = (Calendar) today.clone();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            if (selected.getTimeInMillis() == yesterday.getTimeInMillis()) {
                label = "Hier";
            } else if (selected.getTimeInMillis() == tomorrow.getTimeInMillis()) {
                label = "Demain";
            } else {
                label = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(date.getTime());
            }
        }
        dateInput.setText(label);
        todayHint.setVisibility(android.view.View.GONE);
    }

    private void save() {
        Double w;
        try {
            w = Double.parseDouble(weightInput.getText().toString().trim().replace(',', '.'));
        } catch (Exception e) {
            w = null;
        }
        if (!WeightDatabase.isValidWeight(w)) {
            weightInput.setError("Entrez un poids valide.");
            return;
        }
        String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime());
        new WeightDatabase(this).save(key, w);
        Toast.makeText(this, "Mesure enregistrée.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
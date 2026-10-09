package fr.bbodin.courbedepoids;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMeasurementFragment extends Fragment {
    private TextView dateInput;
    private EditText weightInput;
    private final Calendar date = Calendar.getInstance();

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_add_measurement, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        dateInput = view.findViewById(R.id.date_input);
        weightInput = view.findViewById(R.id.weight_input);
        updateDate();
        dateInput.setOnClickListener(v -> openDatePicker());
        view.findViewById(R.id.save_measurement).setOnClickListener(v -> save());
    }

    private void openDatePicker() {
        new DatePickerDialog(requireActivity(), (picker, year, month, day) -> {
            date.set(year, month, day);
            date.set(Calendar.HOUR_OF_DAY, 0);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            updateDate();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDate() {
        Calendar selected = (Calendar) date.clone();
        selected.set(Calendar.HOUR_OF_DAY, 0); selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0); selected.set(Calendar.MILLISECOND, 0);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        String label;
        if (selected.getTimeInMillis() == today.getTimeInMillis()) label = "Aujourd'hui";
        else {
            Calendar yesterday = (Calendar) today.clone(); yesterday.add(Calendar.DAY_OF_YEAR, -1);
            Calendar tomorrow = (Calendar) today.clone(); tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            if (selected.getTimeInMillis() == yesterday.getTimeInMillis()) label = "Hier";
            else if (selected.getTimeInMillis() == tomorrow.getTimeInMillis()) label = "Demain";
            else label = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(date.getTime());
        }
        dateInput.setText(label);
    }

    private void save() {
        Double value;
        try { value = Double.parseDouble(weightInput.getText().toString().trim().replace(',', '.')); }
        catch (Exception e) { value = null; }
        if (!WeightDatabase.isValidWeight(value)) {
            weightInput.setError("Entrez un poids valide.");
            return;
        }
        String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime());
        new WeightDatabase(requireActivity()).save(key, value);
        Toast.makeText(requireActivity(), "Mesure enregistrée.", Toast.LENGTH_SHORT).show();
        ((MainActivity) requireActivity()).showTab(0);
    }
}
package fr.bbodin.courbedepoids;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.graphics.Color;
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
import java.util.Locale;

public class AddMeasurementFragment extends Fragment {
    private TextView dateInput;
    private EditText weightInput, customWaterInput, sportDurationInput, sportDistanceInput;
    private android.widget.RadioGroup sportTypeGroup;
    private View weightForm, waterForm, sportForm, weightMode, waterMode, sportMode;
    private final Calendar date = Calendar.getInstance();

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.activity_add_measurement, container, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        dateInput = view.findViewById(R.id.date_input);
        weightInput = view.findViewById(R.id.weight_input);
        customWaterInput = view.findViewById(R.id.water_custom_input);
        weightForm = view.findViewById(R.id.weight_form);
        waterForm = view.findViewById(R.id.water_form);
        sportForm=view.findViewById(R.id.sport_form);sportMode=view.findViewById(R.id.sport_mode_button);sportDurationInput=view.findViewById(R.id.sport_duration_input);sportDistanceInput=view.findViewById(R.id.sport_distance_input);sportTypeGroup=view.findViewById(R.id.sport_type_group);
        weightMode = view.findViewById(R.id.weight_mode_button);
        waterMode = view.findViewById(R.id.water_mode_button);
        updateDate();
        dateInput.setOnClickListener(v -> openDatePicker());
        weightMode.setOnClickListener(v -> showMode(true));
        waterMode.setOnClickListener(v -> showMode(1));
        sportMode.setOnClickListener(v -> showMode(2));
        sportTypeGroup.setOnCheckedChangeListener((group,checkedId)->{int vis=checkedId==R.id.sport_type_run?View.VISIBLE:View.GONE;sportDistanceInput.setVisibility(vis);view.findViewById(R.id.sport_distance_label).setVisibility(vis);});
        view.findViewById(R.id.save_measurement).setOnClickListener(v -> saveWeight());
        view.findViewById(R.id.water_250).setOnClickListener(v -> saveWater(250));
        view.findViewById(R.id.water_330).setOnClickListener(v -> saveWater(330));
        view.findViewById(R.id.water_500).setOnClickListener(v -> saveWater(500));
        view.findViewById(R.id.save_custom_water).setOnClickListener(v -> saveCustomWater());
        view.findViewById(R.id.save_sport).setOnClickListener(v->saveSport());
        showMode(0);
    }

    private void showMode(int mode){weightForm.setVisibility(mode==0?View.VISIBLE:View.GONE);waterForm.setVisibility(mode==1?View.VISIBLE:View.GONE);sportForm.setVisibility(mode==2?View.VISIBLE:View.GONE);styleMode(weightMode,mode==0);styleMode(waterMode,mode==1);styleMode(sportMode,mode==2);}

    private void styleMode(View mode, boolean selected) {
        mode.setBackgroundResource(selected ? R.drawable.bg_primary_button : R.drawable.bg_input);
        if (mode instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) mode;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setTextColor(selected ? Color.WHITE : Color.rgb(20, 36, 58));
                if (child instanceof ImageView) ((ImageView) child).setColorFilter(selected ? Color.WHITE : Color.rgb(20, 123, 239));
            }
        }
    }

    private void openDatePicker() {
        new DatePickerDialog(getActivity(), (picker, year, month, day) -> {
            date.set(year, month, day);
            date.set(Calendar.HOUR_OF_DAY, 0); date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0);
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

    private String dateKey() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()); }

    private void saveWeight() {
        Double value;
        try { value = Double.parseDouble(weightInput.getText().toString().trim().replace(',', '.')); }
        catch (Exception e) { value = null; }
        if (!WeightDatabase.isValidWeight(value)) {
            weightInput.setError("Entrez un poids valide.");
            return;
        }
        new WeightDatabase(getActivity()).save(dateKey(), value);
        Toast.makeText(getActivity(), "Mesure enregistrée.", Toast.LENGTH_SHORT).show();
        ((MainActivity) getActivity()).showTab(0);
    }

    private void saveCustomWater() {
        Integer amount;
        try { amount = Integer.parseInt(customWaterInput.getText().toString().trim()); }
        catch (Exception e) { amount = null; }
        if (amount == null || !WeightDatabase.isValidWaterAmount(amount)) {
            customWaterInput.setError("Entrez une quantité entre 1 et 10 000 ml.");
            return;
        }
        saveWater(amount);
    }

    private void saveSport(){int id=sportTypeGroup.getCheckedRadioButtonId();String type=id==R.id.sport_type_yoga?"Yoga":id==R.id.sport_type_run?"Course":id==R.id.sport_type_climb?"Escalade":"Workout";int duration;try{duration=Integer.parseInt(sportDurationInput.getText().toString().trim());}catch(Exception e){duration=-1;}double distance=0;if(type.equals("Course")&&!sportDistanceInput.getText().toString().trim().isEmpty())try{distance=Double.parseDouble(sportDistanceInput.getText().toString().trim().replace(',','.'));}catch(Exception e){distance=-1;}if(!WeightDatabase.isValidSport(type,duration,distance)){if(duration<1||duration>1440)sportDurationInput.setError("Durée entre 1 et 1440 minutes.");else sportDistanceInput.setError("Distance invalide (0 à 1000 km).");return;}new WeightDatabase(getActivity()).saveSport(dateKey(),type,duration,distance);Toast.makeText(getActivity(),type+" enregistré ("+duration+" min).",Toast.LENGTH_SHORT).show();((MainActivity)getActivity()).showTab(0);}
    private void saveWater(int amountMl) {
        new WeightDatabase(getActivity()).saveWater(dateKey(), amountMl);
        Toast.makeText(getActivity(), amountMl + " ml d'eau enregistrés.", Toast.LENGTH_SHORT).show();
        ((MainActivity) getActivity()).showTab(0);
    }
}

package fr.bbodin.courbedepoids;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
    private TextView dateInput, timeInput;
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
        LinearLayout dateCard=(LinearLayout)dateInput.getParent();timeInput=new TextView(getActivity());timeInput.setTextSize(16);timeInput.setTextColor(Color.rgb(20,36,58));timeInput.setGravity(android.view.Gravity.CENTER_VERTICAL);timeInput.setPadding(dp(14),0,dp(14),0);timeInput.setBackgroundResource(R.drawable.bg_input);timeInput.setClickable(true);timeInput.setFocusable(true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(52));tp.topMargin=dp(8);dateCard.addView(timeInput,tp);
        weightInput = view.findViewById(R.id.weight_input);
        customWaterInput = view.findViewById(R.id.water_custom_input);
        weightForm = view.findViewById(R.id.weight_form);
        waterForm = view.findViewById(R.id.water_form);
        sportForm=view.findViewById(R.id.sport_form);sportMode=view.findViewById(R.id.sport_mode_button);sportDurationInput=view.findViewById(R.id.sport_duration_input);sportDistanceInput=view.findViewById(R.id.sport_distance_input);sportTypeGroup=view.findViewById(R.id.sport_type_group);
        weightMode = view.findViewById(R.id.weight_mode_button);
        waterMode = view.findViewById(R.id.water_mode_button);
        updateDate(); updateTime();
        timeInput.setOnClickListener(v -> openTimePicker());
        dateInput.setOnClickListener(v -> openDatePicker());
        weightMode.setOnClickListener(v -> showMode(0));
        waterMode.setOnClickListener(v -> showMode(1));
        sportMode.setOnClickListener(v -> showMode(2));
        sportTypeGroup.setOnCheckedChangeListener((group,checkedId)->{int vis=checkedId==R.id.sport_type_run?View.VISIBLE:View.GONE;sportDistanceInput.setVisibility(vis);view.findViewById(R.id.sport_distance_label).setVisibility(vis);});
        view.findViewById(R.id.save_measurement).setOnClickListener(v -> saveWeight());
        view.findViewById(R.id.water_250).setOnClickListener(v -> saveWater(250));
        view.findViewById(R.id.water_330).setOnClickListener(v -> saveWater(330));
        view.findViewById(R.id.water_500).setOnClickListener(v -> saveWater(500));
        view.findViewById(R.id.save_custom_water).setOnClickListener(v -> saveCustomWater());
        view.findViewById(R.id.save_sport).setOnClickListener(v->saveSport());
        addConfiguredTypes(view);
        showMode(0);
    }

    private void addConfiguredTypes(View root) {
        java.util.List<org.json.JSONObject> types = new LoggingTypeRegistry(getActivity()).getTypes();
        if (types.isEmpty()) return;
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(getActivity());
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout choices = new LinearLayout(getActivity()); choices.setOrientation(LinearLayout.HORIZONTAL);
        for (org.json.JSONObject type : types) {
            LinearLayout item = new LinearLayout(getActivity()); item.setGravity(android.view.Gravity.CENTER);
            item.setPadding(dp(14), 0, dp(14), 0); item.setBackgroundResource(R.drawable.bg_input); item.setClickable(true);
            TextView label = new TextView(getActivity()); label.setText(type.optString("icon", "•") + " " + type.optString("label")); label.setTextColor(Color.rgb(20,36,58)); item.addView(label);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(50)); p.setMargins(dp(4),dp(4),dp(4),dp(4)); choices.addView(item,p);
            item.setOnClickListener(v -> showConfiguredForm(type));
        }
        scroll.addView(choices);
        ((ViewGroup)weightMode.getParent().getParent()).addView(scroll,2,new ViewGroup.LayoutParams(-1,-2));
    }

    private void showConfiguredForm(org.json.JSONObject type) {
        LinearLayout form = new LinearLayout(getActivity()); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(18),dp(8),dp(18),0); android.widget.ScrollView formScroll = new android.widget.ScrollView(getActivity()); formScroll.setFillViewport(true); formScroll.addView(form);
        java.util.Map<String,View> controls = new java.util.LinkedHashMap<>();
        org.json.JSONArray fields = type.optJSONArray("fields");
        if (fields != null) for (int i=0;i<fields.length();i++) {
            org.json.JSONObject f=fields.optJSONObject(i); if(f==null) continue;
            String kind=f.optString("type"), id=f.optString("id");
            TextView label=new TextView(getActivity()); label.setText(f.optString("label")); label.setTextColor(Color.rgb(20,36,58)); form.addView(label);
            View control;
            if(kind.equals("select")) {
                android.widget.Spinner sp=new android.widget.Spinner(getActivity()); java.util.List<String> options=new java.util.ArrayList<>();
                org.json.JSONArray opts=f.optJSONArray("options"); if(opts!=null)for(int j=0;j<opts.length();j++)options.add(opts.optString(j));
                sp.setAdapter(new android.widget.ArrayAdapter<>(getActivity(),android.R.layout.simple_spinner_dropdown_item,options)); control=sp;
            } else if(kind.equals("boolean")) { android.widget.CheckBox cb=new android.widget.CheckBox(getActivity());cb.setText(f.optString("label"));control=cb; }
            else { EditText input=new EditText(getActivity());input.setHint(f.optString("label"));input.setSingleLine(!kind.equals("text"));input.setInputType(kind.equals("number")?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT);control=input; }
            form.addView(control); controls.put(id,control);
        }
        Calendar selected=(Calendar)date.clone();
        new AlertDialog.Builder(getActivity()).setTitle(type.optString("icon","")+" "+type.optString("label")).setView(formScroll).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(dialog,which)->{
            try {
                org.json.JSONObject payload=new org.json.JSONObject();
                if(fields!=null)for(int i=0;i<fields.length();i++){
                    org.json.JSONObject f=fields.optJSONObject(i);if(f==null)continue;String id=f.optString("id"),kind=f.optString("type");View c=controls.get(id);Object value;
                    if(c instanceof android.widget.Spinner)value=((android.widget.Spinner)c).getSelectedItem();
                    else if(c instanceof android.widget.CheckBox)value=((android.widget.CheckBox)c).isChecked();
                    else value=((EditText)c).getText().toString().trim();
                    if(f.optBoolean("required",false)&&(value==null||String.valueOf(value).trim().isEmpty()))throw new IllegalArgumentException("Champ requis : "+f.optString("label"));
                    if(kind.equals("number")&&value!=null&&!String.valueOf(value).isEmpty())payload.put(id,Double.parseDouble(String.valueOf(value).replace(',','.')));
                    else if(value!=null)payload.put(id,value);
                }
                new WeightDatabase(getActivity()).saveGenericEvent(type.optString("id"),new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(selected.getTime()),selected.getTimeInMillis(),payload.toString());
                Toast.makeText(getActivity(),type.optString("label")+" enregistré.",Toast.LENGTH_SHORT).show();((MainActivity)getActivity()).showTab(0);
            } catch(Exception e) { Toast.makeText(getActivity(),e.getMessage()==null?"Données invalides":e.getMessage(),Toast.LENGTH_LONG).show(); }
        }).show();
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
            int hour = date.get(Calendar.HOUR_OF_DAY);
            int minute = date.get(Calendar.MINUTE);
            date.set(year, month, day, hour, minute, 0);
            date.set(Calendar.MILLISECOND, 0);
            updateDate();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openTimePicker(){new TimePickerDialog(getActivity(),(p,h,m)->{date.set(Calendar.HOUR_OF_DAY,h);date.set(Calendar.MINUTE,m);date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);updateTime();},date.get(Calendar.HOUR_OF_DAY),date.get(Calendar.MINUTE),true).show();}
    private void updateTime(){timeInput.setText("Heure de mesure · "+new SimpleDateFormat("HH:mm",Locale.FRANCE).format(date.getTime())+" ("+java.util.TimeZone.getDefault().getID()+")");}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private long measuredAt(){return date.getTimeInMillis();}
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
        new WeightDatabase(getActivity()).save(dateKey(), value, measuredAt());
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

    private void saveSport(){int id=sportTypeGroup.getCheckedRadioButtonId();String type=id==R.id.sport_type_yoga?"Yoga":id==R.id.sport_type_run?"Course":id==R.id.sport_type_climb?"Escalade":"Workout";int duration;try{duration=Integer.parseInt(sportDurationInput.getText().toString().trim());}catch(Exception e){duration=-1;}double distance=0;if(type.equals("Course")&&!sportDistanceInput.getText().toString().trim().isEmpty())try{distance=Double.parseDouble(sportDistanceInput.getText().toString().trim().replace(',','.'));}catch(Exception e){distance=-1;}if(!WeightDatabase.isValidSport(type,duration,distance)){if(duration<1||duration>1440)sportDurationInput.setError("Durée entre 1 et 1440 minutes.");else sportDistanceInput.setError("Distance invalide (0 à 1000 km).");return;}new WeightDatabase(getActivity()).saveSport(dateKey(),type,duration,distance,measuredAt());Toast.makeText(getActivity(),type+" enregistré ("+duration+" min).",Toast.LENGTH_SHORT).show();((MainActivity)getActivity()).showTab(0);}
    private void saveWater(int amountMl) {
        new WeightDatabase(getActivity()).saveWater(dateKey(), amountMl, measuredAt());
        Toast.makeText(getActivity(), amountMl + " ml d'eau enregistrés.", Toast.LENGTH_SHORT).show();
        ((MainActivity) getActivity()).showTab(0);
    }
}

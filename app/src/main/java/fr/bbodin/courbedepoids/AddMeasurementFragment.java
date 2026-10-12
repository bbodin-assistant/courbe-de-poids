package fr.bbodin.courbedepoids;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddMeasurementFragment extends Fragment {
    private static final int BLUE = Color.rgb(20, 123, 239);
    private static final int INK = Color.rgb(20, 36, 58);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private TextView dateInput, timeInput, selectedTypeHeader, saveButton;
    private LinearLayout categoryContainer, subcategoryContainer, formContainer;
    private final Calendar date = Calendar.getInstance();
    private final Map<String, List<JSONObject>> categories = new LinkedHashMap<>();
    private final List<String> categoryOrder = new ArrayList<>();
    private String selectedCategory;
    private JSONObject selectedType;
    private ConfiguredLoggingForm activeForm;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.activity_add_measurement, parent, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        dateInput = view.findViewById(R.id.date_input);
        timeInput = view.findViewById(R.id.time_input);
        categoryContainer = view.findViewById(R.id.category_container);
        subcategoryContainer = view.findViewById(R.id.subcategory_container);
        formContainer = view.findViewById(R.id.configured_form_container);
        selectedTypeHeader = view.findViewById(R.id.selected_type_header);
        saveButton = view.findViewById(R.id.save_event_button);

        dateInput.setOnClickListener(v -> openDatePicker());
        timeInput.setOnClickListener(v -> openTimePicker());
        saveButton.setOnClickListener(v -> saveSelectedEvent());
        updateDate();
        updateTime();

        for (JSONObject type : new LoggingTypeRegistry(getActivity()).getTypes()) {
            String category = type.optString("category", "other");
            if (!categories.containsKey(category)) {
                categories.put(category, new ArrayList<>());
                categoryOrder.add(category);
            }
            categories.get(category).add(type);
        }
        if (!categoryOrder.isEmpty()) {
            selectedCategory = categoryOrder.get(0);
            buildCategoryChoices();
            buildSubcategoryChoices();
            if (!categories.get(selectedCategory).isEmpty()) selectType(categories.get(selectedCategory).get(0));
        } else {
            selectedTypeHeader.setText("Aucun type configuré");
            saveButton.setEnabled(false);
            saveButton.setAlpha(0.5f);
        }
    }

    private void buildCategoryChoices() {
        categoryContainer.removeAllViews();
        for (String category : categoryOrder) {
            boolean selected = category.equals(selectedCategory);
            TextView chip = chip(categoryIcon(category) + "  " + categoryLabel(category), selected, true);
            chip.setContentDescription(categoryLabel(category));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                buildCategoryChoices();
                buildSubcategoryChoices();
                List<JSONObject> types = categories.get(selectedCategory);
                if (types != null && !types.isEmpty()) selectType(types.get(0));
            });
            addChip(categoryContainer, chip);
        }
    }

    private void buildSubcategoryChoices() {
        subcategoryContainer.removeAllViews();
        List<JSONObject> types = categories.get(selectedCategory);
        if (types == null) return;
        for (JSONObject type : types) {
            boolean selected = selectedType != null && selectedType.optString("id").equals(type.optString("id"));
            TextView chip = chip(type.optString("icon", "•") + "  " + type.optString("label"), selected, false);
            chip.setOnClickListener(v -> selectType(type));
            addChip(subcategoryContainer, chip);
        }
    }

    private TextView chip(String label, boolean selected, boolean category) {
        TextView chip = new TextView(getActivity());
        chip.setText(label);
        chip.setTextSize(14);
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(selected ? Color.WHITE : INK);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(dp(category ? 16 : 14), 0, dp(category ? 16 : 14), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? BLUE : Color.WHITE);
        bg.setCornerRadius(dp(category ? 22 : 16));
        bg.setStroke(dp(1), selected ? BLUE : Color.rgb(220, 231, 245));
        chip.setBackground(bg);
        chip.setElevation(selected ? dp(2) : 0);
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }

    private void addChip(LinearLayout parent, TextView chip) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(46));
        params.setMargins(0, 0, dp(8), dp(4));
        parent.addView(chip, params);
    }

    private void selectType(JSONObject type) {
        selectedType = type;
        buildSubcategoryChoices();
        selectedTypeHeader.setText(type.optString("icon", "•") + "  " + type.optString("label"));
        formContainer.removeAllViews();
        activeForm = new ConfiguredLoggingForm(getActivity(), type, null);
        formContainer.addView(activeForm.getView(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        saveButton.setText("Enregistrer " + type.optString("label").toLowerCase(Locale.FRANCE));
        saveButton.setEnabled(true);
        saveButton.setAlpha(1f);
    }

    private void saveSelectedEvent() {
        if (selectedType == null || activeForm == null) return;
        try {
            JSONObject payload = activeForm.readPayload();
            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime());
            new WeightDatabase(getActivity()).saveGenericEvent(
                    selectedType.optString("id"), dateKey, date.getTimeInMillis(), payload.toString());
            Toast.makeText(getActivity(), selectedType.optString("label") + " enregistré.", Toast.LENGTH_SHORT).show();
            ((MainActivity) getActivity()).showTab(0);
        } catch (Exception error) {
            Toast.makeText(getActivity(), error.getMessage() == null
                    ? "Données invalides" : error.getMessage(), Toast.LENGTH_LONG).show();
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

    private void openTimePicker() {
        new TimePickerDialog(getActivity(), (picker, hour, minute) -> {
            date.set(Calendar.HOUR_OF_DAY, hour);
            date.set(Calendar.MINUTE, minute);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            updateTime();
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), true).show();
    }

    private void updateTime() {
        timeInput.setText("◷  " + new SimpleDateFormat("HH:mm", Locale.FRANCE).format(date.getTime())
                + "     ·     " + java.util.TimeZone.getDefault().getID());
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
            if (selected.getTimeInMillis() == yesterday.getTimeInMillis()) label = "Hier";
            else if (selected.getTimeInMillis() == tomorrow.getTimeInMillis()) label = "Demain";
            else label = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(date.getTime());
        }
        dateInput.setText("▦  " + label);
    }

    private String categoryLabel(String category) {
        switch (category) {
            case "body": return "Corps";
            case "health": return "Santé";
            case "nutrition": return "Nutrition";
            case "activity": return "Activité";
            default: return category;
        }
    }

    private String categoryIcon(String category) {
        switch (category) {
            case "body": return "⚖";
            case "health": return "♡";
            case "nutrition": return "◉";
            case "activity": return "↗";
            default: return "•";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

package fr.bbodin.courbedepoids;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds and validates a polished input form from a generic logging type definition. */
final class ConfiguredLoggingForm {
    private static final int BLUE = Color.rgb(20, 123, 239);
    private static final int INK = Color.rgb(20, 36, 58);
    private final Context context;
    private final JSONArray fields;
    private final Map<String, View> controls = new LinkedHashMap<>();
    private final LinearLayout content;

    ConfiguredLoggingForm(Context context, JSONObject type, JSONObject initialValues) {
        this.context = context;
        this.fields = type.optJSONArray("fields");
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(4), 0, dp(4));

        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.optJSONObject(i);
                if (field == null) continue;
                String id = field.optString("id");
                String kind = field.optString("type");
                Object initial = initialValues == null ? null : initialValues.opt(id);

                if ("boolean".equals(kind)) {
                    CheckBox check = new CheckBox(context);
                    check.setText(field.optString("label"));
                    check.setTextColor(Color.rgb(20, 36, 58));
                    check.setTextSize(15);
                    check.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(20, 123, 239)));
                    check.setChecked(initial instanceof Boolean ? (Boolean) initial
                            : initial != null && "true".equalsIgnoreCase(String.valueOf(initial)));
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
                    cp.topMargin = dp(4);
                    cp.bottomMargin = dp(6);
                    content.addView(check, cp);
                    controls.put(id, check);
                    continue;
                }

                TextView label = new TextView(context);
                label.setText(field.optString("label") + (field.optBoolean("required", false) ? "  *" : ""));
                label.setTextColor(Color.rgb(51, 65, 85));
                label.setTextSize(14);
                label.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(i == 0 ? 4 : 14);
                lp.bottomMargin = dp(6);
                content.addView(label, lp);

                View control;
                if ("choice_grid".equals(kind)) {
                    control = new ChoiceGrid(field.optJSONArray("options"), initial,
                            Math.max(1, field.optInt("columns", 3)));
                } else if ("select".equals(kind)) {
                    Spinner spinner = new Spinner(context);
                    List<String> options = new ArrayList<>();
                    options.add("Choisir…");
                    JSONArray configured = field.optJSONArray("options");
                    if (configured != null) {
                        for (int j = 0; j < configured.length(); j++) options.add(configured.optString(j));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            context, android.R.layout.simple_spinner_item, options) {
                        @Override public View getView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getView(position, convertView, parent);
                            view.setTextColor(Color.rgb(20, 36, 58));
                            view.setTextSize(15);
                            view.setPadding(dp(12), dp(8), dp(12), dp(8));
                            return view;
                        }
                        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                            view.setTextColor(Color.rgb(20, 36, 58));
                            view.setPadding(dp(14), dp(12), dp(14), dp(12));
                            return view;
                        }
                    };
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    if (initial != null) {
                        for (int j = 1; j < options.size(); j++) {
                            if (options.get(j).equals(String.valueOf(initial))) { spinner.setSelection(j); break; }
                        }
                    }
                    control = spinner;
                } else {
                    EditText input = new EditText(context);
                    input.setTextColor(Color.rgb(20, 36, 58));
                    input.setHintTextColor(Color.rgb(148, 163, 184));
                    input.setTextSize(16);
                    input.setPadding(dp(14), dp(10), dp(14), dp(10));
                    input.setHint("Saisir " + field.optString("label").toLowerCase(java.util.Locale.FRANCE));
                    input.setSingleLine(!"text".equals(kind));
                    input.setMinHeight(dp("text".equals(kind) ? 88 : 52));
                    input.setBackground(roundedInput());
                    if ("number".equals(kind)) {
                        input.setInputType(InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    } else {
                        input.setInputType(InputType.TYPE_CLASS_TEXT
                                | ("text".equals(kind) ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
                    }
                    if (initial != null && initial != JSONObject.NULL) input.setText(String.valueOf(initial));
                    control = input;
                }
                LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        "text".equals(kind) ? dp(96) : dp(52));
                content.addView(control, controlParams);
                controls.put(id, control);
            }
        }
    }

    View getView() { return content; }

    JSONObject readPayload() throws Exception {
        JSONObject payload = new JSONObject();
        if (fields == null) return payload;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) continue;
            String id = field.optString("id");
            String kind = field.optString("type");
            View control = controls.get(id);
            Object value = null;
            boolean hasValue = false;
            if (control instanceof ChoiceGrid) {
                value = ((ChoiceGrid) control).getSelectedValue();
                hasValue = value != null && value != JSONObject.NULL;
            } else if (control instanceof Spinner) {
                Object selected = ((Spinner) control).getSelectedItem();
                if (selected != null && !String.valueOf(selected).trim().isEmpty()
                        && !"Choisir…".equals(String.valueOf(selected))) {
                    value = String.valueOf(selected);
                    hasValue = true;
                }
            } else if (control instanceof CheckBox) {
                value = ((CheckBox) control).isChecked();
                hasValue = true;
            } else if (control instanceof EditText) {
                String text = ((EditText) control).getText().toString().trim();
                if (!text.isEmpty()) {
                    if ("number".equals(kind)) {
                        double number;
                        try { number = Double.parseDouble(text.replace(',', '.')); }
                        catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Nombre invalide : " + field.optString("label"));
                        }
                        if (field.has("min") && number < field.optDouble("min"))
                            throw new IllegalArgumentException(field.optString("label") + " doit être supérieur ou égal à " + field.opt("min"));
                        if (field.has("max") && number > field.optDouble("max"))
                            throw new IllegalArgumentException(field.optString("label") + " doit être inférieur ou égal à " + field.opt("max"));
                        value = number;
                    } else value = text;
                    hasValue = true;
                }
            }
            if (field.optBoolean("required", false) && !hasValue) {
                throw new IllegalArgumentException("Champ requis : " + field.optString("label"));
            }
            if (hasValue) payload.put(id, value);
        }
        return payload;
    }


    /** Renders JSON-configured options as image-like, tappable cards. */
    private final class ChoiceGrid extends LinearLayout {
        private final JSONArray options;
        private final List<LinearLayout> tiles = new ArrayList<>();
        private final int columns;
        private int selectedIndex = -1;

        ChoiceGrid(JSONArray options, Object initial, int columns) {
            super(context);
            this.options = options == null ? new JSONArray() : options;
            this.columns = Math.min(columns, Math.max(1, this.options.length()));
            setOrientation(VERTICAL);
            setPadding(0, dp(2), 0, dp(2));
            for (int start = 0; start < this.options.length(); start += this.columns) {
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(8);
                addView(row, rowParams);
                for (int index = start; index < Math.min(start + this.columns, this.options.length()); index++) {
                    JSONObject option = this.options.optJSONObject(index);
                    if (option == null) continue;
                    final int optionIndex = index;
                    LinearLayout tile = new LinearLayout(context);
                    tile.setOrientation(VERTICAL);
                    tile.setGravity(Gravity.CENTER);
                    tile.setPadding(dp(4), dp(8), dp(4), dp(8));
                    tile.setMinimumHeight(dp(90));
                    tile.setBackground(tileBackground(false));
                    tile.setClickable(true);
                    tile.setFocusable(true);
                    TextView icon = new TextView(context);
                    icon.setText(option.optString("icon", "•"));
                    icon.setTextSize(25);
                    icon.setGravity(Gravity.CENTER);
                    TextView optionLabel = new TextView(context);
                    optionLabel.setText(option.optString("label"));
                    optionLabel.setTextSize(12);
                    optionLabel.setGravity(Gravity.CENTER);
                    optionLabel.setTextColor(INK);
                    optionLabel.setMaxLines(2);
                    tile.addView(icon, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    labelParams.topMargin = dp(3);
                    tile.addView(optionLabel, labelParams);
                    LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(90), 1f);
                    tileParams.setMargins(dp(3), 0, dp(3), 0);
                    row.addView(tile, tileParams);
                    tiles.add(tile);
                    tile.setOnClickListener(v -> {
                        selectedIndex = optionIndex;
                        refreshSelection();
                    });
                    Object optionValue = option.has("value") ? option.opt("value") : option.optString("label");
                    if (initial != null && initial != JSONObject.NULL
                            && String.valueOf(optionValue).equals(String.valueOf(initial))) selectedIndex = optionIndex;
                }
            }
            refreshSelection();
        }

        Object getSelectedValue() {
            if (selectedIndex < 0 || selectedIndex >= options.length()) return null;
            JSONObject option = options.optJSONObject(selectedIndex);
            if (option == null) return null;
            Object value = option.opt("value");
            return value == null || value == JSONObject.NULL ? option.optString("label") : value;
        }

        private void refreshSelection() {
            for (int i = 0; i < tiles.size(); i++) {
                boolean selected = i == selectedIndex;
                tiles.get(i).setBackground(tileBackground(selected));
                tiles.get(i).setElevation(selected ? dp(2) : 0);
                if (tiles.get(i).getChildCount() > 1) {
                    TextView label = (TextView) tiles.get(i).getChildAt(1);
                    label.setTextColor(selected ? BLUE : INK);
                    label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    private GradientDrawable tileBackground(boolean selected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(selected ? Color.rgb(232, 243, 255) : Color.WHITE);
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(selected ? 2 : 1), selected ? BLUE : Color.rgb(220, 231, 245));
        return shape;
    }

    private GradientDrawable roundedInput() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(255, 255, 255));
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), Color.rgb(220, 231, 245));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

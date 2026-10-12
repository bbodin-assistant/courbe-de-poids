package fr.bbodin.courbedepoids;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds and validates a logging form entirely from a registry definition. */
final class ConfiguredLoggingForm {
    private final Context context;
    private final JSONArray fields;
    private final Map<String, View> controls = new LinkedHashMap<>();
    private final ScrollView scroll;

    ConfiguredLoggingForm(Context context, JSONObject type, JSONObject initialValues) {
        this.context = context;
        this.fields = type.optJSONArray("fields");
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, dp(8), pad, dp(8));
        if (fields != null) {
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.optJSONObject(i);
                if (field == null) continue;
                String id = field.optString("id");
                String kind = field.optString("type");
                Object initial = initialValues == null ? null : initialValues.opt(id);
                TextView label = new TextView(context);
                label.setText(field.optString("label"));
                label.setTextColor(android.graphics.Color.rgb(20, 36, 58));
                content.addView(label);
                View control;
                if ("select".equals(kind)) {
                    Spinner spinner = new Spinner(context);
                    List<String> options = new ArrayList<>();
                    options.add("");
                    JSONArray configured = field.optJSONArray("options");
                    if (configured != null) {
                        for (int j = 0; j < configured.length(); j++) options.add(configured.optString(j));
                    }
                    spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, options));
                    if (initial != null) {
                        for (int j = 1; j < options.size(); j++) {
                            if (options.get(j).equals(String.valueOf(initial))) { spinner.setSelection(j); break; }
                        }
                    }
                    control = spinner;
                } else if ("boolean".equals(kind)) {
                    CheckBox check = new CheckBox(context);
                    check.setText(field.optString("label"));
                    check.setChecked(initial instanceof Boolean ? (Boolean) initial
                            : initial != null && "true".equalsIgnoreCase(String.valueOf(initial)));
                    control = check;
                } else {
                    EditText input = new EditText(context);
                    input.setHint(field.optString("label"));
                    input.setSingleLine(!"text".equals(kind));
                    if ("number".equals(kind)) {
                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    } else {
                        input.setInputType(InputType.TYPE_CLASS_TEXT | ("text".equals(kind) ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
                    }
                    if (initial != null && initial != JSONObject.NULL) input.setText(String.valueOf(initial));
                    control = input;
                }
                content.addView(control);
                controls.put(id, control);
            }
        }
        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(content);
    }

    View getView() { return scroll; }

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
            if (control instanceof Spinner) {
                Object selected = ((Spinner) control).getSelectedItem();
                if (selected != null && !String.valueOf(selected).trim().isEmpty()) {
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
                        catch (NumberFormatException e) { throw new IllegalArgumentException("Nombre invalide : " + field.optString("label")); }
                        if (field.has("min") && number < field.optDouble("min")) throw new IllegalArgumentException(field.optString("label") + " doit être supérieur ou égal à " + field.opt("min"));
                        if (field.has("max") && number > field.optDouble("max")) throw new IllegalArgumentException(field.optString("label") + " doit être inférieur ou égal à " + field.opt("max"));
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

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

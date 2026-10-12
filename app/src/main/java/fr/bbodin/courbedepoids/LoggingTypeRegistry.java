package fr.bbodin.courbedepoids;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LoggingTypeRegistry {
    private final List<JSONObject> types = new ArrayList<>();

    public LoggingTypeRegistry(Context context) {
        try (InputStream in = context.getAssets().open("logging_types.json");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            JSONArray definitions = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8))
                    .optJSONArray("types");
            if (definitions != null) {
                for (int i = 0; i < definitions.length(); i++) {
                    JSONObject type = definitions.optJSONObject(i);
                    if (type != null && isValidType(type)) types.add(type);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Invalid logging_types.json", e);
        }
    }

    private boolean isValidType(JSONObject type) {
        if (!isValidTypeId(type.optString("id", ""))
                || type.optString("label", "").trim().isEmpty()) return false;
        JSONArray fields = type.optJSONArray("fields");
        if (fields == null) return false;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null || !isValidFieldDefinition(
                    field.optString("id", ""),
                    field.optString("label", ""),
                    field.optString("type", ""),
                    field.optJSONArray("options") != null)) return false;
        }
        return true;
    }

    static boolean isValidTypeId(String id) {
        return id != null && id.matches("[a-z][a-z0-9_]{0,39}");
    }

    static boolean isSupportedFieldType(String type) {
        return "text".equals(type) || "number".equals(type) || "select".equals(type)
                || "boolean".equals(type) || "choice_grid".equals(type);
    }

    static boolean isValidFieldDefinition(String id, String label, String type, boolean hasOptions) {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,39}")
                || label == null || label.trim().isEmpty() || !isSupportedFieldType(type)) return false;
        return !("select".equals(type) || "choice_grid".equals(type)) || hasOptions;
    }

    public List<JSONObject> getTypes() {
        return new ArrayList<>(types);
    }

    public JSONObject getType(String id) {
        for (JSONObject type : types) if (type.optString("id").equals(id)) return type;
        return null;
    }
}

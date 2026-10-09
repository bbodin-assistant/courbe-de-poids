package fr.bbodin.courbedepoids;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class BackupManager {
    public static final int FORMAT_VERSION = 1;
    private static final String PREFS = "settings";
    private BackupManager() {}

    public static JSONObject createBackup(Context context, WeightDatabase database) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("format", "courbe-de-poids-backup");
        root.put("formatVersion", FORMAT_VERSION);
        root.put("createdAt", System.currentTimeMillis());
        JSONArray measurements = new JSONArray();
        for (WeightDatabase.Measurement measurement : database.all()) {
            JSONObject item = new JSONObject();
            item.put("date", measurement.date);
            item.put("weightKg", measurement.weight);
            measurements.put(item);
        }
        root.put("measurements", measurements);
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONObject settings = new JSONObject();
        settings.put("reminderEnabled", preferences.getBoolean("reminder_enabled", false));
        settings.put("reminderHour", preferences.getInt("reminder_hour", 8));
        settings.put("reminderMinute", preferences.getInt("reminder_minute", 0));
        root.put("settings", settings);
        return root;
    }

    public static int restoreBackup(Context context, WeightDatabase database, JSONObject root)
            throws JSONException {
        if (!"courbe-de-poids-backup".equals(root.optString("format"))
                || root.optInt("formatVersion", -1) != FORMAT_VERSION) {
            throw new JSONException("Format de sauvegarde non pris en charge");
        }
        JSONArray measurements = root.optJSONArray("measurements");
        JSONObject settings = root.optJSONObject("settings");
        if (measurements == null || settings == null) throw new JSONException("Sauvegarde incomplète");
        List<WeightDatabase.Measurement> validated = new ArrayList<>();
        for (int i = 0; i < measurements.length(); i++) {
            JSONObject item = measurements.optJSONObject(i);
            if (item == null) throw new JSONException("Mesure invalide");
            String date = item.optString("date", "");
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new JSONException("Date invalide");
            double weight = item.optDouble("weightKg", Double.NaN);
            if (!WeightDatabase.isValidWeight(weight)) throw new JSONException("Poids invalide");
            validated.add(new WeightDatabase.Measurement(date, weight));
        }
        int hour = settings.optInt("reminderHour", 8);
        int minute = settings.optInt("reminderMinute", 0);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new JSONException("Configuration du rappel invalide");
        }
        database.replaceAll(validated);
        boolean reminderEnabled = settings.optBoolean("reminderEnabled", false);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("reminder_enabled", reminderEnabled)
                .putInt("reminder_hour", hour)
                .putInt("reminder_minute", minute).apply();
        if (reminderEnabled) ReminderScheduler.schedule(context, hour, minute);
        else ReminderScheduler.cancel(context);
        return validated.size();
    }
}

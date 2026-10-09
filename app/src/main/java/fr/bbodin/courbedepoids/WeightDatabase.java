package fr.bbodin.courbedepoids;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class WeightDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "weight.db";
    private static final int DB_VERSION = 2;
    private static final String TABLE = "measurements";
    private static final String WATER_TABLE = "water_events";
    private static final double MAX_WEIGHT_KG = 500.0;
    private final Context context;

    public static class Measurement {
        public final String date;
        public final double weight;
        public Measurement(String date, double weight) { this.date = date; this.weight = weight; }
    }

    public static class WaterEvent {
        public final long id;
        public final String date;
        public final int amountMl;
        public final long createdAt;
        public WaterEvent(long id, String date, int amountMl, long createdAt) {
            this.id = id; this.date = date; this.amountMl = amountMl; this.createdAt = createdAt;
        }
    }

    public WeightDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    public static boolean isValidWeight(Double weight) {
        return weight != null && Double.isFinite(weight) && weight > 0.0 && weight <= MAX_WEIGHT_KG;
    }

    public static boolean isValidWaterAmount(Integer amountMl) {
        return amountMl != null && amountMl >= 1 && amountMl <= 10000;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (date TEXT PRIMARY KEY, weight REAL NOT NULL)");
        createWaterTable(db);
    }

    private void createWaterTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + WATER_TABLE + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, amount_ml INTEGER NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_events_date ON " + WATER_TABLE + " (date)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createWaterTable(db);
    }

    public void save(String date, double weight) {
        if (!isValidWeight(weight)) throw new IllegalArgumentException("Invalid weight");
        ContentValues v = new ContentValues(); v.put("date", date); v.put("weight", weight);
        getWritableDatabase().insertWithOnConflict(TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public void saveWater(String date, int amountMl) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}") || !isValidWaterAmount(amountMl))
            throw new IllegalArgumentException("Invalid water event");
        ContentValues values = new ContentValues();
        values.put("date", date); values.put("amount_ml", amountMl); values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow(WATER_TABLE, null, values);
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public List<WaterEvent> allWaterEvents() {
        List<WaterEvent> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(WATER_TABLE, new String[]{"_id", "date", "amount_ml", "created_at"},
                null, null, null, null, "date ASC, created_at ASC, _id ASC");
        try { while (c.moveToNext()) result.add(new WaterEvent(c.getLong(0), c.getString(1), c.getInt(2), c.getLong(3))); }
        finally { c.close(); }
        return result;
    }

    public int totalWaterForDate(String date) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount_ml), 0) FROM " + WATER_TABLE + " WHERE date=?", new String[]{date});
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    public void replaceWaterEvents(List<WaterEvent> events) {
        SQLiteDatabase database = getWritableDatabase(); database.beginTransaction();
        try {
            database.delete(WATER_TABLE, null, null);
            for (WaterEvent event : events) {
                if (event == null || event.date == null || !event.date.matches("\\d{4}-\\d{2}-\\d{2}") || !isValidWaterAmount(event.amountMl) || event.createdAt < 0)
                    throw new IllegalArgumentException("Invalid water event");
                ContentValues values = new ContentValues(); values.put("date", event.date);
                values.put("amount_ml", event.amountMl); values.put("created_at", event.createdAt);
                database.insertOrThrow(WATER_TABLE, null, values);
            }
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public void replaceAll(List<Measurement> measurements) {
        SQLiteDatabase database = getWritableDatabase(); database.beginTransaction();
        try {
            database.delete(TABLE, null, null);
            for (Measurement measurement : measurements) {
                if (measurement == null || !isValidWeight(measurement.weight) || measurement.date == null || !measurement.date.matches("\\d{4}-\\d{2}-\\d{2}"))
                    throw new IllegalArgumentException("Invalid measurement");
                ContentValues values = new ContentValues(); values.put("date", measurement.date); values.put("weight", measurement.weight);
                database.insertOrThrow(TABLE, null, values);
            }
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public void delete(String date) {
        getWritableDatabase().delete(TABLE, "date=?", new String[]{date});
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public Measurement get(String date) {
        Cursor c = getReadableDatabase().query(TABLE, new String[]{"date", "weight"}, "date=?", new String[]{date}, null, null, null);
        try { return c.moveToFirst() ? new Measurement(c.getString(0), c.getDouble(1)) : null; } finally { c.close(); }
    }

    public List<Measurement> all() {
        List<Measurement> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE, new String[]{"date", "weight"}, null, null, null, null, "date ASC");
        try { while (c.moveToNext()) result.add(new Measurement(c.getString(0), c.getDouble(1))); } finally { c.close(); }
        return result;
    }
}

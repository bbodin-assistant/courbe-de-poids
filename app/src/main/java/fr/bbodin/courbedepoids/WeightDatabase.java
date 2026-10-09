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
    private static final int DB_VERSION = 1;
    private static final String TABLE = "measurements";
    private static final double MAX_WEIGHT_KG = 500.0;

    public static class Measurement {
        public final String date;
        public final double weight;
        public Measurement(String date, double weight) {
            this.date = date;
            this.weight = weight;
        }
    }

    public WeightDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static boolean isValidWeight(Double weight) {
        return weight != null && Double.isFinite(weight) && weight > 0.0 && weight <= MAX_WEIGHT_KG;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (date TEXT PRIMARY KEY, weight REAL NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            switch (version) {
                default:
                    // No schema migration is currently required. Add each future
                    // migration as a new case so existing data is preserved.
                    break;
            }
        }
    }

    public void save(String date, double weight) {
        if (!isValidWeight(weight)) throw new IllegalArgumentException("Invalid weight");
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("weight", weight);
        getWritableDatabase().insertWithOnConflict(TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        AutoBackupScheduler.backupIfEnabled(getContext());
    }

    public void replaceAll(List<Measurement> measurements) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            database.delete(TABLE, null, null);
            for (Measurement measurement : measurements) {
                if (measurement == null || !isValidWeight(measurement.weight)
                        || measurement.date == null
                        || !measurement.date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new IllegalArgumentException("Invalid measurement");
                }
                ContentValues values = new ContentValues();
                values.put("date", measurement.date);
                values.put("weight", measurement.weight);
                database.insertOrThrow(TABLE, null, values);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        AutoBackupScheduler.backupIfEnabled(getContext());
    }

    public void delete(String date) {
        getWritableDatabase().delete(TABLE, "date=?", new String[]{date});
        AutoBackupScheduler.backupIfEnabled(getContext());
    }

    public Measurement get(String date) {
        Cursor c = getReadableDatabase().query(TABLE, new String[]{"date","weight"},
                "date=?", new String[]{date}, null, null, null);
        try {
            return c.moveToFirst() ? new Measurement(c.getString(0), c.getDouble(1)) : null;
        } finally { c.close(); }
    }

    public List<Measurement> all() {
        List<Measurement> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE, new String[]{"date","weight"},
                null, null, null, null, "date ASC");
        try {
            while (c.moveToNext()) result.add(new Measurement(c.getString(0), c.getDouble(1)));
        } finally { c.close(); }
        return result;
    }
}

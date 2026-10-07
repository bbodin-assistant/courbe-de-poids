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

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (date TEXT PRIMARY KEY, weight REAL NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public void save(String date, double weight) {
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("weight", weight);
        getWritableDatabase().insertWithOnConflict(TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delete(String date) {
        getWritableDatabase().delete(TABLE, "date=?", new String[]{date});
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
                null, null, null, null, "date DESC");
        try {
            while (c.moveToNext()) result.add(new Measurement(c.getString(0), c.getDouble(1)));
        } finally { c.close(); }
        return result;
    }
}

package fr.bbodin.courbedepoids;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class WeightDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "weight.db";
    private static final int DB_VERSION = 6;
    private static final String TABLE = "measurements";
    private static final String WATER_TABLE = "water_events";
    private static final String SPORT_TABLE = "sport_events";
    private static final String GENERIC_TABLE = "generic_events";
    private static final double MAX_WEIGHT_KG = 500.0;
    private final Context context;

    public static class Measurement {
        public final String date;
        public final double weight;
        public final long measuredAt;
        public Measurement(String date,double weight){this(date,weight,localMidnight(date));}
        public Measurement(String date,double weight,long measuredAt){this.date=date;this.weight=weight;this.measuredAt=measuredAt;}
        private static long localMidnight(String date){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);f.setTimeZone(java.util.TimeZone.getDefault());java.util.Date d=f.parse(date);return d==null?System.currentTimeMillis():d.getTime();}catch(Exception e){return System.currentTimeMillis();}}
    }
    public static class WaterEvent {
        public final long id; public final String date; public final int amountMl; public final long createdAt;
        public WaterEvent(long id,String date,int amountMl,long createdAt){this.id=id;this.date=date;this.amountMl=amountMl;this.createdAt=createdAt;}
    }
    public static class SportEvent {
        public final long id; public final String date,type; public final int durationMinutes; public final double distanceKm; public final long createdAt;
        public SportEvent(long id,String date,String type,int durationMinutes,double distanceKm,long createdAt){this.id=id;this.date=date;this.type=type;this.durationMinutes=durationMinutes;this.distanceKm=distanceKm;this.createdAt=createdAt;}
    }
    public static class GenericEvent {
        public final long id; public final String typeId,date,payload; public final long createdAt;
        public GenericEvent(long id,String typeId,String date,long createdAt,String payload){this.id=id;this.typeId=typeId;this.date=date;this.createdAt=createdAt;this.payload=payload;}
    }

    public WeightDatabase(Context context){super(context,DB_NAME,null,DB_VERSION);this.context=context.getApplicationContext();}
    public static boolean isValidWeight(Double weight){return weight!=null&&Double.isFinite(weight)&&weight>0.0&&weight<=MAX_WEIGHT_KG;}
    public static boolean isValidWaterAmount(Integer amountMl){return amountMl!=null&&amountMl>=1&&amountMl<=10000;}
    public static boolean isValidSport(String type,int durationMinutes,double distanceKm){return type!=null&&(type.equals("Yoga")||type.equals("Course")||type.equals("Escalade")||type.equals("Workout"))&&durationMinutes>=1&&durationMinutes<=1440&&Double.isFinite(distanceKm)&&distanceKm>=0&&distanceKm<=1000&&(type.equals("Course")||distanceKm==0);}

    @Override public void onCreate(SQLiteDatabase db){
        createGenericEventsTable(db);
    }
    private void createWaterTable(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS "+WATER_TABLE+" (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, amount_ml INTEGER NOT NULL, created_at INTEGER NOT NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_events_date ON "+WATER_TABLE+" (date)");}
    private void createSportTable(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS "+SPORT_TABLE+" (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, type TEXT NOT NULL, duration_minutes INTEGER NOT NULL, distance_km REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_sport_events_date ON "+SPORT_TABLE+" (date)");}
    private void createGenericEventsTable(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS "+GENERIC_TABLE+" (_id INTEGER PRIMARY KEY AUTOINCREMENT, type_id TEXT NOT NULL, date TEXT NOT NULL, created_at INTEGER NOT NULL, payload TEXT NOT NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_generic_events_date ON "+GENERIC_TABLE+" (date, created_at)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_generic_events_type ON "+GENERIC_TABLE+" (type_id)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2)createWaterTable(db);
        if(oldVersion<3)createSportTable(db);
        if(oldVersion<4){db.execSQL("ALTER TABLE "+TABLE+" ADD COLUMN measured_at INTEGER NOT NULL DEFAULT 0");Cursor c=db.query(TABLE,new String[]{"date"},null,null,null,null,null);try{while(c.moveToNext()){String date=c.getString(0);ContentValues v=new ContentValues();v.put("measured_at",Measurement.localMidnight(date));db.update(TABLE,v,"date=?",new String[]{date});}}finally{c.close();}}
        if(oldVersion<5)createGenericEventsTable(db);
        if(oldVersion<6)migrateLegacyEvents(db);
    }

    private void migrateLegacyEvents(SQLiteDatabase db){
        Cursor c=db.query(TABLE,new String[]{"date","weight","measured_at"},null,null,null,null,null);
        try{while(c.moveToNext()){String date=c.getString(0);JSONObject payload=new JSONObject();payload.put("weight_kg",c.getDouble(1));insertGeneric(db,"weight",date,c.getLong(2),payload.toString(),true);}}catch(Exception e){throw new IllegalStateException("Unable to migrate weight measurements",e);}finally{c.close();}
        c=db.query(WATER_TABLE,new String[]{"_id","date","amount_ml","created_at"},null,null,null,null,null);
        try{while(c.moveToNext()){JSONObject payload=new JSONObject();payload.put("amount_ml",c.getInt(2));insertGeneric(db,"water",c.getString(1),c.getLong(3),payload.toString(),false);}}catch(Exception e){throw new IllegalStateException("Unable to migrate water events",e);}finally{c.close();}
        c=db.query(SPORT_TABLE,new String[]{"_id","date","type","duration_minutes","distance_km","created_at"},null,null,null,null,null);
        try{while(c.moveToNext()){JSONObject payload=new JSONObject();payload.put("activity",c.getString(2));payload.put("duration_minutes",c.getInt(3));payload.put("distance_km",c.getDouble(4));insertGeneric(db,"sport",c.getString(1),c.getLong(5),payload.toString(),false);}}catch(Exception e){throw new IllegalStateException("Unable to migrate sport events",e);}finally{c.close();}
        db.delete(TABLE,null,null);db.delete(WATER_TABLE,null,null);db.delete(SPORT_TABLE,null,null);
    }

    private long insertGeneric(SQLiteDatabase db,String typeId,String date,long createdAt,String payload,boolean replaceWeight){
        if(replaceWeight)db.delete(GENERIC_TABLE,"type_id=? AND date=?",new String[]{typeId,date});
        ContentValues values=new ContentValues();values.put("type_id",typeId);values.put("date",date);values.put("created_at",createdAt);values.put("payload",payload);
        return db.insertOrThrow(GENERIC_TABLE,null,values);
    }
    static boolean isValidGenericEvent(String typeId,String date,long createdAt,String payload){
        if(!LoggingTypeRegistry.isValidTypeId(typeId)||date==null||!date.matches("\\d{4}-\\d{2}-\\d{2}")||createdAt<0||payload==null)return false;
        try{JSONObject p=new JSONObject(payload);
            if("weight".equals(typeId))return isValidWeight(p.optDouble("weight_kg",Double.NaN));
            if("water".equals(typeId))return isValidWaterAmount(p.optInt("amount_ml",-1));
            if("sport".equals(typeId))return isValidSport(p.optString("activity",""),p.optInt("duration_minutes",-1),p.optDouble("distance_km",0));
            return true;
        }catch(Exception e){return false;}
    }
    private void writeGeneric(SQLiteDatabase db,String typeId,String date,long createdAt,String payload,boolean replaceWeight){
        if(!isValidGenericEvent(typeId,date,createdAt,payload))throw new IllegalArgumentException("Invalid generic event");
        insertGeneric(db,typeId,date,createdAt,payload,replaceWeight);
    }
    public long saveGenericEvent(String typeId,String date,long createdAt,String payload){
        if(!isValidGenericEvent(typeId,date,createdAt,payload))throw new IllegalArgumentException("Invalid generic event");
        SQLiteDatabase db=getWritableDatabase();long id;
        if("weight".equals(typeId))db.delete(GENERIC_TABLE,"type_id=? AND date=?",new String[]{typeId,date});
        ContentValues v=new ContentValues();v.put("type_id",typeId);v.put("date",date);v.put("created_at",createdAt);v.put("payload",payload);id=db.insertOrThrow(GENERIC_TABLE,null,v);AutoBackupScheduler.backupIfEnabled(context);return id;
    }
    public void updateGenericEvent(long id,String typeId,String date,long createdAt,String payload){
        if(!isValidGenericEvent(typeId,date,createdAt,payload))throw new IllegalArgumentException("Invalid generic event");
        SQLiteDatabase db=getWritableDatabase();
        if("weight".equals(typeId)){Cursor c=db.query(GENERIC_TABLE,new String[]{"_id"},"type_id=? AND date=? AND _id<>?",new String[]{typeId,date,String.valueOf(id)},null,null,null);try{if(c.moveToFirst())throw new IllegalArgumentException("Une mesure de poids existe déjà pour cette date.");}finally{c.close();}}
        ContentValues v=new ContentValues();v.put("type_id",typeId);v.put("date",date);v.put("created_at",createdAt);v.put("payload",payload);
        if(db.update(GENERIC_TABLE,v,"_id=? AND type_id=?",new String[]{String.valueOf(id),typeId})==0)throw new IllegalArgumentException("Generic event not found");AutoBackupScheduler.backupIfEnabled(context);
    }
    public List<GenericEvent> allGenericEvents(){List<GenericEvent> out=new ArrayList<>();Cursor c=getReadableDatabase().query(GENERIC_TABLE,new String[]{"_id","type_id","date","created_at","payload"},null,null,null,null,"date DESC, created_at DESC, _id DESC");try{while(c.moveToNext())out.add(new GenericEvent(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3),c.getString(4)));}finally{c.close();}return out;}
    public GenericEvent getGenericEvent(long id,String typeId){Cursor c=getReadableDatabase().query(GENERIC_TABLE,new String[]{"_id","type_id","date","created_at","payload"},"_id=? AND type_id=?",new String[]{String.valueOf(id),typeId},null,null,null);try{return c.moveToFirst()?new GenericEvent(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3),c.getString(4)):null;}finally{c.close();}}

    public void save(String date,double weight){save(date,weight,Measurement.localMidnight(date));}
    public void save(String date,double weight,long measuredAt){JSONObject p=new JSONObject();try{p.put("weight_kg",weight);}catch(Exception e){throw new IllegalArgumentException(e);}saveGenericEvent("weight",date,measuredAt,p.toString());}
    public void updateMeasurementTime(String date,long measuredAt){Measurement m=get(date);if(m==null)throw new IllegalArgumentException("Measurement not found");JSONObject p=new JSONObject();try{p.put("weight_kg",m.weight);}catch(Exception e){throw new IllegalArgumentException(e);}updateWeightByDate(date,measuredAt,p.toString());}
    private void updateWeightByDate(String date,long measuredAt,String payload){if(measuredAt<0||!isValidGenericEvent("weight",date,measuredAt,payload))throw new IllegalArgumentException("Invalid weight measurement");ContentValues v=new ContentValues();v.put("created_at",measuredAt);v.put("payload",payload);if(getWritableDatabase().update(GENERIC_TABLE,v,"type_id='weight' AND date=?",new String[]{date})==0)throw new IllegalArgumentException("Measurement not found");AutoBackupScheduler.backupIfEnabled(context);}

    public void saveWater(String date,int amountMl){saveWater(date,amountMl,System.currentTimeMillis());}
    public void saveWater(String date,int amountMl,long measuredAt){JSONObject p=new JSONObject();try{p.put("amount_ml",amountMl);}catch(Exception e){throw new IllegalArgumentException(e);}saveGenericEvent("water",date,measuredAt,p.toString());}
    public void updateWaterEvent(long id,int amountMl,long measuredAt){GenericEvent e=getGenericEvent(id,"water");if(e==null)throw new IllegalArgumentException("Water event not found");JSONObject p=new JSONObject();try{p.put("amount_ml",amountMl);}catch(Exception x){throw new IllegalArgumentException(x);}updateGenericEvent(id,"water",e.date,measuredAt,p.toString());}
    public void updateWaterEvent(long id,int amountMl){GenericEvent e=getGenericEvent(id,"water");if(e==null)throw new IllegalArgumentException("Water event not found");updateWaterEvent(id,amountMl,e.createdAt);}
    public List<WaterEvent> allWaterEvents(){List<WaterEvent> out=new ArrayList<>();for(GenericEvent e:allGenericEvents())if("water".equals(e.typeId))try{JSONObject p=new JSONObject(e.payload);out.add(new WaterEvent(e.id,e.date,p.getInt("amount_ml"),e.createdAt));}catch(Exception ignored){}return out;}

    public void saveSport(String date,String type,int durationMinutes,double distanceKm){saveSport(date,type,durationMinutes,distanceKm,System.currentTimeMillis());}
    public void saveSport(String date,String type,int durationMinutes,double distanceKm,long measuredAt){JSONObject p=new JSONObject();try{p.put("activity",type);p.put("duration_minutes",durationMinutes);p.put("distance_km",distanceKm);}catch(Exception e){throw new IllegalArgumentException(e);}saveGenericEvent("sport",date,measuredAt,p.toString());}
    public List<SportEvent> allSportEvents(){List<SportEvent> out=new ArrayList<>();for(GenericEvent e:allGenericEvents())if("sport".equals(e.typeId))try{JSONObject p=new JSONObject(e.payload);out.add(new SportEvent(e.id,e.date,p.getString("activity"),p.getInt("duration_minutes"),p.optDouble("distance_km",0),e.createdAt));}catch(Exception ignored){}return out;}
    public void updateSportEvent(long id,String type,int durationMinutes,double distanceKm){updateSportEvent(id,type,durationMinutes,distanceKm,null);}
    public void updateSportEvent(long id,String type,int durationMinutes,double distanceKm,Long measuredAt){GenericEvent e=getGenericEvent(id,"sport");if(e==null)throw new IllegalArgumentException("Sport event not found");JSONObject p=new JSONObject();try{p.put("activity",type);p.put("duration_minutes",durationMinutes);p.put("distance_km",distanceKm);}catch(Exception x){throw new IllegalArgumentException(x);}updateGenericEvent(id,"sport",e.date,measuredAt==null?e.createdAt:measuredAt,p.toString());}
    public int totalSportMinutesForDate(String date){int total=0;for(SportEvent e:allSportEvents())if(date.equals(e.date))total+=e.durationMinutes;return total;}
    public int totalWaterForDate(String date){int total=0;for(WaterEvent e:allWaterEvents())if(date.equals(e.date))total+=e.amountMl;return total;}

    public void replaceSportEvents(List<SportEvent> events){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete(GENERIC_TABLE,"type_id='sport'",null);for(SportEvent e:events){if(e==null)throw new IllegalArgumentException("Invalid sport event");JSONObject p=new JSONObject();p.put("activity",e.type);p.put("duration_minutes",e.durationMinutes);p.put("distance_km",e.distanceKm);writeGeneric(db,"sport",e.date,e.createdAt,p.toString(),false);}db.setTransactionSuccessful();}catch(Exception e){throw new IllegalArgumentException("Invalid sport event",e);}finally{db.endTransaction();}AutoBackupScheduler.backupIfEnabled(context);}
    public void replaceWaterEvents(List<WaterEvent> events){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete(GENERIC_TABLE,"type_id='water'",null);for(WaterEvent e:events){if(e==null)throw new IllegalArgumentException("Invalid water event");JSONObject p=new JSONObject();p.put("amount_ml",e.amountMl);writeGeneric(db,"water",e.date,e.createdAt,p.toString(),false);}db.setTransactionSuccessful();}catch(Exception e){throw new IllegalArgumentException("Invalid water event",e);}finally{db.endTransaction();}AutoBackupScheduler.backupIfEnabled(context);}
    public void replaceAll(List<Measurement> measurements){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete(GENERIC_TABLE,"type_id='weight'",null);for(Measurement m:measurements){if(m==null)throw new IllegalArgumentException("Invalid measurement");JSONObject p=new JSONObject();p.put("weight_kg",m.weight);writeGeneric(db,"weight",m.date,m.measuredAt,p.toString(),true);}db.setTransactionSuccessful();}catch(Exception e){throw new IllegalArgumentException("Invalid measurement",e);}finally{db.endTransaction();}AutoBackupScheduler.backupIfEnabled(context);}

    public void replaceCustomGenericEvents(List<GenericEvent> events){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete(GENERIC_TABLE,"type_id NOT IN ('weight','water','sport')",null);
            for(GenericEvent e:events){
                if(e==null||"weight".equals(e.typeId)||"water".equals(e.typeId)||"sport".equals(e.typeId))throw new IllegalArgumentException("Invalid custom event");
                writeGeneric(db,e.typeId,e.date,e.createdAt,e.payload,false);
            }
            db.setTransactionSuccessful();
        }catch(Exception e){throw new IllegalArgumentException("Invalid generic events",e);}
        finally{db.endTransaction();}
        AutoBackupScheduler.backupIfEnabled(context);
    }

    public void deleteEvent(String typeId,String eventKey){
        if(typeId==null||eventKey==null||eventKey.trim().isEmpty())throw new IllegalArgumentException("Event type and key are required");
        int deleted;
        if("weight".equals(typeId))deleted=getWritableDatabase().delete(GENERIC_TABLE,"type_id=? AND date=?",new String[]{typeId,eventKey});
        else deleted=getWritableDatabase().delete(GENERIC_TABLE,"type_id=? AND _id=?",new String[]{typeId,eventKey});
        if(deleted>0)AutoBackupScheduler.backupIfEnabled(context);
    }
    public void updateMeasurement(String date,double weight,long measuredAt){JSONObject p=new JSONObject();try{p.put("weight_kg",weight);}catch(Exception e){throw new IllegalArgumentException(e);}updateWeightByDate(date,measuredAt,p.toString());}
    public Measurement get(String date){Cursor c=getReadableDatabase().query(GENERIC_TABLE,new String[]{"date","created_at","payload"},"type_id='weight' AND date=?",new String[]{date},null,null,null);try{if(!c.moveToFirst())return null;return new Measurement(c.getString(0),new JSONObject(c.getString(2)).getDouble("weight_kg"),c.getLong(1));}catch(Exception e){return null;}finally{c.close();}}
    public List<Measurement> all(){List<Measurement> out=new ArrayList<>();for(GenericEvent e:allGenericEvents())if("weight".equals(e.typeId))try{out.add(new Measurement(e.date,new JSONObject(e.payload).getDouble("weight_kg"),e.createdAt));}catch(Exception ignored){}java.util.Collections.sort(out,(a,b)->a.date.compareTo(b.date));return out;}
}

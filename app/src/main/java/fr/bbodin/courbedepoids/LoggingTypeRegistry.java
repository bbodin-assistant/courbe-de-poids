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
 private final List<JSONObject> types=new ArrayList<>();
 public LoggingTypeRegistry(Context c){try(InputStream in=c.getAssets().open("logging_types.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);JSONArray a=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)).optJSONArray("types");if(a!=null)for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t!=null&&valid(t))types.add(t);}}catch(Exception e){throw new IllegalStateException("Invalid logging_types.json",e);}}
 private boolean valid(JSONObject t){if(!t.optString("id","").matches("[a-z][a-z0-9_]{0,39}")||t.optString("label","").trim().isEmpty())return false;JSONArray a=t.optJSONArray("fields");if(a==null)return false;for(int i=0;i<a.length();i++){JSONObject f=a.optJSONObject(i);if(f==null||f.optString("id","").isEmpty()||f.optString("label","").isEmpty())return false;String k=f.optString("type","");if(!k.equals("text")&&!k.equals("number")&&!k.equals("select")&&!k.equals("boolean"))return false;if(k.equals("select")&&f.optJSONArray("options")==null)return false;}return true;}
 public List<JSONObject> getTypes(){return new ArrayList<>(types);}
 public JSONObject getType(String id){for(JSONObject t:types)if(t.optString("id").equals(id))return t;return null;}
}
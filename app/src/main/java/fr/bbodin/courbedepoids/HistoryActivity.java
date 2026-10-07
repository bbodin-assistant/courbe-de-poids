package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private LinearLayout list; private WeightDatabase db;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_history);db=new WeightDatabase(this);list=findViewById(R.id.history_list);findViewById(R.id.add_measurement).setOnClickListener(v->startActivity(new Intent(this,AddMeasurementActivity.class)));render();}
    @Override protected void onResume(){super.onResume();if(db!=null)render();}
    private void render(){
        list.removeAllViews(); List<WeightDatabase.Measurement> ms=db.all();
        for(WeightDatabase.Measurement m:ms){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(8,14,8,14);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView t=new TextView(this);t.setText(m.date+"   "+String.format(Locale.FRANCE,"%.1f kg",m.weight));t.setTextSize(18);t.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            Button edit=new Button(this);edit.setText("Modifier");edit.setOnClickListener(v->edit(m));
            Button del=new Button(this);del.setText("Supprimer");del.setOnClickListener(v->delete(m));
            row.addView(t);row.addView(edit);row.addView(del);list.addView(row);
        }
        if(ms.isEmpty()){TextView e=new TextView(this);e.setText("Aucune mesure.");e.setTextSize(18);list.addView(e);}
    }
    private void edit(WeightDatabase.Measurement m){
        final android.widget.EditText input=new android.widget.EditText(this);input.setInputType(2|8192);input.setText(String.format(Locale.FRANCE,"%.1f",m.weight));
        new AlertDialog.Builder(this).setTitle("Modifier "+m.date).setView(input).setPositiveButton("Enregistrer",(d,w)->{
            try{double value=Double.parseDouble(input.getText().toString().replace(',','.'));if(value<=0)throw new Exception();db.save(m.date,value);render();}catch(Exception e){Toast.makeText(this,"Poids invalide.",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("Annuler",null).show();
    }
    private void delete(WeightDatabase.Measurement m){
        new AlertDialog.Builder(this).setTitle("Supprimer la mesure ?").setMessage(m.date+" — "+m.weight+" kg").setPositiveButton("Supprimer",(d,w)->{db.delete(m.date);render();}).setNegativeButton("Annuler",null).show();
    }
}

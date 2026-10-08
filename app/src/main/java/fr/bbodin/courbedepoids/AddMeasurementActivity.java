package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMeasurementActivity extends Activity {
    private EditText dateInput, weightInput;
    private final Calendar date=Calendar.getInstance();
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_add_measurement);
        dateInput=findViewById(R.id.date_input); weightInput=findViewById(R.id.weight_input);
        Button save=findViewById(R.id.save_measurement);
        updateDate();
        dateInput.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{
            date.set(y,m,d); updateDate();
        },date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH)).show());
        save.setOnClickListener(v->save());
    }
    private void updateDate(){ dateInput.setText(new SimpleDateFormat("dd/MM/yyyy",Locale.FRANCE).format(date.getTime())); }
    private void save(){
        Double w;
        try{w=Double.parseDouble(weightInput.getText().toString().trim().replace(',','.'));}catch(Exception e){w=null;}
        if(!WeightDatabase.isValidWeight(w)){weightInput.setError("Entrez un poids valide.");return;}
        String key=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.getTime());
        new WeightDatabase(this).save(key,w);
        Toast.makeText(this,"Mesure enregistrée.",Toast.LENGTH_SHORT).show(); finish();
    }
}

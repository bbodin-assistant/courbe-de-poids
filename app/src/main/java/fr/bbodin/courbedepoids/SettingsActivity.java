package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private Switch enabled; private TextView time;
    private android.content.SharedPreferences prefs;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);setContentView(R.layout.activity_settings);
        prefs=getSharedPreferences("settings",MODE_PRIVATE);enabled=findViewById(R.id.reminder_enabled);time=findViewById(R.id.reminder_time);
        enabled.setChecked(prefs.getBoolean("reminder_enabled",false));updateTimeText();
        time.setOnClickListener(v->pickTime());
        enabled.setOnCheckedChangeListener((button,checked)->{
            prefs.edit().putBoolean("reminder_enabled",checked).apply();
            if(checked) ReminderScheduler.schedule(this,prefs.getInt("reminder_hour",8),prefs.getInt("reminder_minute",0));
            else ReminderScheduler.cancel(this);
        });
    }
    private void updateTimeText(){time.setText(String.format(Locale.FRANCE,"Heure du rappel : %02d:%02d",prefs.getInt("reminder_hour",8),prefs.getInt("reminder_minute",0)));}
    private void pickTime(){
        new TimePickerDialog(this,(v,h,m)->{
            prefs.edit().putInt("reminder_hour",h).putInt("reminder_minute",m).apply();updateTimeText();
            if(enabled.isChecked()) ReminderScheduler.schedule(this,h,m);
        },prefs.getInt("reminder_hour",8),prefs.getInt("reminder_minute",0),true).show();
    }
}

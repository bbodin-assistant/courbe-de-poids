package fr.bbodin.courbedepoids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class AcceptanceCriteriaInstrumentedTest {
    private static final String PACKAGE = "fr.bbodin.courbedepoids";
    private Context context;

    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule notificationPermissionRule =
            Build.VERSION.SDK_INT >= 33
                    ? GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
                    : GrantPermissionRule.grant();

    @Before
    public void resetState() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight.db");
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().clear().commit();
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancelAll();
    }

    private WeightDatabase db() {
        return new WeightDatabase(context);
    }

    private String dateOffset(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private String shell(String command) throws Exception {
        ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand(command);
        try (FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Test
    public void criterion01_enregistrerLePoidsDuJour() {
        onView(withId(R.id.weight_input)).perform(replaceText("72,3"));
        onView(withId(R.id.save_today)).perform(click());
        onView(withId(R.id.today_status)).check(matches(withText("Mesure du jour : 72,3 kg")));
        assertNotNull(db().get(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime())));
    }

    @Test
    public void criterion02_enregistrerUneMesurePassee() {
        String past = dateOffset(-2);
        db().save(past, 71.4);
        ActivityScenario<HistoryActivity> scenario = ActivityScenario.launch(HistoryActivity.class);
        onView(withText(allOf(withText(past + "   71,4 kg")))).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void criterion03_uneDateNePossedeQuUneMesure() {
        String date = dateOffset(-1);
        db().save(date, 70.0);
        db().save(date, 71.0);
        List<WeightDatabase.Measurement> all = db().all();
        int count = 0;
        for (WeightDatabase.Measurement m : all) if (date.equals(m.date)) {
            count++;
            assertEquals(71.0, m.weight, 0.001);
        }
        assertEquals(1, count);
    }

    @Test
    public void criterion04_modifierUneMesureExistante() {
        String date = dateOffset(-1);
        db().save(date, 70.0);
        ActivityScenario<HistoryActivity> scenario = ActivityScenario.launch(HistoryActivity.class);
        onView(withText("Modifier")).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(clearText(), replaceText("75,4"));
        onView(withText("Enregistrer")).perform(click());
        onView(withText(date + "   75,4 kg")).check(matches(isDisplayed()));
        scenario.close();
    }

    @Test
    public void criterion05_supprimerUneMesure() {
        String date = dateOffset(-1);
        db().save(date, 70.0);
        ActivityScenario<HistoryActivity> scenario = ActivityScenario.launch(HistoryActivity.class);
        onView(withText("Supprimer")).perform(click());
        onView(withText("Supprimer")).perform(click());
        assertTrue(db().get(date) == null);
        scenario.close();
    }

    @Test
    public void criterion06_historiqueCorrectementAffiche() {
        String oldest = dateOffset(-3);
        String middle = dateOffset(-2);
        String newest = dateOffset(-1);
        db().save(oldest, 70.0);
        db().save(middle, 71.0);
        db().save(newest, 72.0);

        ActivityScenario<HistoryActivity> scenario = ActivityScenario.launch(HistoryActivity.class);
        onView(withId(R.id.history_list)).check((view, noException) -> {
            if (noException != null) throw noException;
            LinearLayout list = (LinearLayout) view;
            assertEquals(3, list.getChildCount());
            String first = ((android.widget.TextView) ((LinearLayout) list.getChildAt(0)).getChildAt(0)).getText().toString();
            String second = ((android.widget.TextView) ((LinearLayout) list.getChildAt(1)).getChildAt(0)).getText().toString();
            String third = ((android.widget.TextView) ((LinearLayout) list.getChildAt(2)).getChildAt(0)).getText().toString();
            assertTrue(first.startsWith(newest));
            assertTrue(second.startsWith(middle));
            assertTrue(third.startsWith(oldest));
        });
        scenario.close();
    }

    @Test
    public void criterion07_courbeRepresenteLesMesures() throws Exception {
        db().save(dateOffset(-3), 70.0);
        db().save(dateOffset(-1), 72.0);

        mainRule.getScenario().recreate();
        onView(withId(R.id.weight_chart)).check(matches(isDisplayed()));

        final Object[] dataHolder = new Object[1];
        onView(withId(R.id.weight_chart)).check((view, noException) -> {
            if (noException != null) throw noException;
            try {
                java.lang.reflect.Field field = WeightChartView.class.getDeclaredField("data");
                field.setAccessible(true);
                dataHolder[0] = field.get(view);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        assertNotNull(dataHolder[0]);
        assertEquals(2, ((List<?>) dataHolder[0]).size());
    }

    @Test
    public void criterion08_activerDesactiverRappel() {
        ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class);
        onView(withId(R.id.reminder_enabled)).perform(click());
        assertTrue(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("reminder_enabled", false));
        onView(withId(R.id.reminder_enabled)).perform(click());
        assertFalse(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("reminder_enabled", true));
        scenario.close();
    }

    @Test
    public void criterion09_choisirHeureDuRappel() {
        ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class);
        onView(withId(R.id.reminder_time)).perform(click());
        onView(isAssignableFrom(TimePicker.class)).perform(new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return isAssignableFrom(TimePicker.class);
            }
            @Override public String getDescription() { return "Définir 07:35"; }
            @Override public void perform(androidx.test.espresso.UiController uiController, android.view.View view) {
                TimePicker picker = (TimePicker) view;
                picker.setHour(7);
                picker.setMinute(35);
            }
        });
        onView(withText("OK")).perform(click());
        assertEquals(7, context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("reminder_hour", -1));
        assertEquals(35, context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("reminder_minute", -1));
        scenario.close();
    }

    @Test
    public void criterion10_rappelDeclencheUneNotification() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putBoolean("reminder_enabled", true).putInt("reminder_hour", 8).putInt("reminder_minute", 0).commit();

        new ReminderReceiver().onReceive(context, new Intent(context, ReminderReceiver.class));

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        Notification[] notifications = nm.getActiveNotifications() != null
                ? java.util.Arrays.stream(nm.getActiveNotifications()).map(s -> s.getNotification()).toArray(Notification[]::new)
                : new Notification[0];
        assertTrue("La notification de rappel doit être publiée", notifications.length > 0);
        assertNotNull(notifications[0].contentIntent);
    }

    @Test
    public void criterion11_notificationOuvreApplication() throws Exception {
        new ReminderReceiver().onReceive(context, new Intent(context, ReminderReceiver.class));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        android.service.notification.StatusBarNotification[] active = nm.getActiveNotifications();
        assertTrue(active.length > 0);
        PendingIntent contentIntent = active[0].getNotification().contentIntent;
        assertNotNull(contentIntent);
        contentIntent.send();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withText("Courbe de poids")).check(matches(isDisplayed()));
    }

    @Test
    public void criterion12_rappelFonctionneApplicationFermee() {
        mainRule.getScenario().close();
        new ReminderReceiver().onReceive(context, new Intent(context, ReminderReceiver.class));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        assertTrue(nm.getActiveNotifications().length > 0);
    }

    @Test
    public void criterion13_donneesConserveesApresFermeture() {
        String date = dateOffset(-1);
        db().save(date, 73.2);
        mainRule.getScenario().close();
        ActivityScenario<MainActivity> reopened = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.weight_chart)).check(matches(isDisplayed()));
        assertEquals(73.2, db().get(date).weight, 0.001);
        reopened.close();
    }

    @Test
    public void criterion14_donneesConserveesApresRedemarrageSimule() throws Exception {
        String date = dateOffset(-1);
        db().save(date, 73.2);
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putBoolean("reminder_enabled", true).putInt("reminder_hour", 8).putInt("reminder_minute", 0).commit();

        context.sendBroadcast(new Intent(Intent.ACTION_BOOT_COMPLETED));
        assertNotNull(db().get(date));
        assertTrue(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("reminder_enabled", false));
    }

    @Test
    public void criterion15_lapplicationEstInstallableEtVersionnee() {
        android.content.pm.ApplicationInfo info = context.getApplicationInfo();
        assertNotNull(info);
        assertEquals(PACKAGE, context.getPackageName());
        assertFalse(BuildConfig.VERSION_NAME.trim().isEmpty());
    }
}

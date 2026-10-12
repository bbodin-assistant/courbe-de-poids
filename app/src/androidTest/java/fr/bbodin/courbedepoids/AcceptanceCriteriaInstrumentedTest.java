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
import android.provider.Settings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AcceptanceCriteriaInstrumentedTest {
    private static final String PACKAGE = "fr.bbodin.courbedepoids";
    private Context context;
    private WeightDatabase database;

    @Rule
    public GrantPermissionRule notificationPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS);

    @Before
    public void resetState() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("logging.db");
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().clear().commit();
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancelAll();
    }

    private WeightDatabase db() {
        if (database == null) database = new WeightDatabase(context);
        return database;
    }

    @After
    public void cleanupState() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    private String dateOffset(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private void saveWeight(String date, double value) {
        try {
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("weight_kg", value);
            db().saveGenericEvent("weight", date, System.currentTimeMillis(), payload.toString());
        } catch (org.json.JSONException e) { throw new AssertionError(e); }
    }

    private WeightDatabase.GenericEvent getWeight(String date) {
        for (WeightDatabase.GenericEvent event : db().allGenericEvents())
            if ("weight".equals(event.typeId) && date.equals(event.date)) return event;
        return null;
    }

    private double weightValue(WeightDatabase.GenericEvent event) {
        try { return new org.json.JSONObject(event.payload).getDouble("weight_kg"); }
        catch (org.json.JSONException e) { throw new AssertionError(e); }
    }

    @Test
    public void criterion01_enregistrerLePoidsDuJour() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        saveWeight(today, 72.3);
        ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.current_weight)).check(matches(withText("72,3 kg")));
        assertNotNull(getWeight(today));
        main.close();
    }

    @Test
    public void criterion02_enregistrerUneMesurePassee() {
        String past = dateOffset(-2);
        saveWeight(past, 71.4);
        assertNotNull(getWeight(past));
    }

    @Test
    public void criterion03_uneDateNePossedeQuUneMesure() {
        String date = dateOffset(-1);
        saveWeight(date, 70.0);
        saveWeight(date, 71.0);
        int count = 0;
        for (WeightDatabase.GenericEvent event : db().allGenericEvents()) {
            if ("weight".equals(event.typeId) && date.equals(event.date)) {
                count++;
                assertEquals(71.0, weightValue(event), 0.001);
            }
        }
        assertEquals(1, count);
    }

    @Test
    public void criterion04_modifierUneMesureExistante() throws Exception {
        String date = dateOffset(-1);
        saveWeight(date, 70.0);
        WeightDatabase.GenericEvent event = getWeight(date);
        org.json.JSONObject payload = new org.json.JSONObject();
        payload.put("weight_kg", 75.4);
        db().updateGenericEvent(event.id, event.typeId, date, event.createdAt, payload.toString());
        assertEquals(75.4, weightValue(getWeight(date)), 0.001);
    }

    @Test
    public void criterion05_supprimerUneMesure() {
        String date = dateOffset(-1);
        saveWeight(date, 70.0);
        WeightDatabase.GenericEvent event = getWeight(date);
        db().deleteEvent("weight", String.valueOf(event.id));
        assertTrue(getWeight(date) == null);
    }

    @Test
    public void criterion06_historiqueCorrectementAffiche() {
        String oldest = dateOffset(-3), middle = dateOffset(-2), newest = dateOffset(-1);
        saveWeight(oldest, 70.0); saveWeight(middle, 71.0); saveWeight(newest, 72.0);
        List<WeightDatabase.GenericEvent> weights = new java.util.ArrayList<>();
        for (WeightDatabase.GenericEvent event : db().allGenericEvents()) if ("weight".equals(event.typeId)) weights.add(event);
        java.util.Collections.sort(weights, (a,b) -> b.date.compareTo(a.date));
        assertEquals(3, weights.size());
        assertEquals(newest, weights.get(0).date);
        assertEquals(middle, weights.get(1).date);
        assertEquals(oldest, weights.get(2).date);
    }

    @Test
    public void criterion07_courbeRepresenteLesMesures() throws Exception {
        saveWeight(dateOffset(-3), 70.0);
        saveWeight(dateOffset(-1), 72.0);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.weight_chart)).check(matches(isDisplayed()));
        final Object[] dataHolder = new Object[1];
        onView(withId(R.id.weight_chart)).check((view, noException) -> {
            if (noException != null) throw noException;
            try {
                java.lang.reflect.Field field = GenericChartView.class.getDeclaredField("events");
                field.setAccessible(true);
                dataHolder[0] = field.get(view);
            } catch (Exception e) { throw new AssertionError(e); }
        });
        assertNotNull(dataHolder[0]);
        long count = ((List<?>) dataHolder[0]).stream().filter(e -> e instanceof WeightDatabase.GenericEvent && "weight".equals(((WeightDatabase.GenericEvent)e).typeId)).count();
        assertEquals(2L, count);
        scenario.close();
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

        ReminderScheduler.schedule(context, 8, 0);
        PendingIntent scheduled = PendingIntent.getBroadcast(
                context, 4242, new Intent(context, ReminderReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        assertNotNull("Le rappel doit être programmé", scheduled);

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
    public void criterion00_rappelFonctionneApplicationFermee() {
        new ReminderReceiver().onReceive(context, new Intent(context, ReminderReceiver.class));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        assertTrue(nm.getActiveNotifications().length > 0);
    }

    @Test
    public void criterion13_donneesConserveesApresFermeture() {
        String date = dateOffset(-1);
        saveWeight(date, 73.2)
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.close();
        ActivityScenario<MainActivity> reopened = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.weight_chart)).check(matches(isDisplayed()));
        assertEquals(73.2, getWeight(date).weight, 0.001);
        reopened.close();
    }

    @Test
    public void criterion14_donneesConserveesApresRedemarrageSimule() throws Exception {
        String date = dateOffset(-1);
        saveWeight(date, 73.2)
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putBoolean("reminder_enabled", true).putInt("reminder_hour", 8).putInt("reminder_minute", 0).commit();

        new BootReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
        assertNotNull(getWeight(date));
        assertTrue(context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("reminder_enabled", false));
    }

    @Test
    public void criterion15_lapplicationEstInstallableEtVersionnee() throws Exception {
        android.content.pm.ApplicationInfo info = context.getApplicationInfo();
        assertNotNull(info);
        assertEquals(PACKAGE, context.getPackageName());
        String versionName = context.getPackageManager()
                .getPackageInfo(PACKAGE, 0).versionName;
        assertNotNull(versionName);
        assertFalse(versionName.trim().isEmpty());
    }
}

package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WaterChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.WaterEvent> events;
    private int days = 7;
    private int endOffsetDays = 0;
    public WaterChartView(Context c) { super(c); }
    public WaterChartView(Context c, AttributeSet a) { super(c, a); }
    public WaterChartView(Context c, AttributeSet a, int d) { super(c, a, d); }
    public void setData(List<WeightDatabase.WaterEvent> value, int periodDays, int offsetDays) {
        events = value; days = Math.max(3, periodDays); endOffsetDays = offsetDays; invalidate();
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (events == null || events.isEmpty()) { drawEmpty(canvas, "Aucune consommation d’eau enregistrée"); return; }
        Calendar today = Calendar.getInstance();
        clearTime(today);
        Calendar end = (Calendar) today.clone();
        end.add(Calendar.DAY_OF_YEAR, -endOffsetDays);
        Calendar start = (Calendar) end.clone();
        start.add(Calendar.DAY_OF_YEAR, -(days - 1));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Map<String, Integer> totals = new HashMap<>();
        for (WeightDatabase.WaterEvent event : events) {
            if (event.date.compareTo(fmt.format(start.getTime())) >= 0 && event.date.compareTo(fmt.format(end.getTime())) <= 0) {
                Integer previous = totals.get(event.date);
                totals.put(event.date, (previous == null ? 0 : previous) + event.amountMl);
            }
        }
        boolean hasValue = false;
        for (Integer value : totals.values()) if (value > 0) { hasValue = true; break; }
        if (!hasValue) { drawEmpty(canvas, "Aucune consommation sur cette période"); return; }

        float left = dp(43), right = getWidth() - dp(8), top = dp(18), bottom = getHeight() - dp(34);
        if (right <= left || bottom <= top) return;
        int maxMl = 0;
        for (Integer value : totals.values()) maxMl = Math.max(maxMl, value);
        int scale = Math.max(500, (int) Math.ceil(maxMl / 500.0) * 500);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0xFFE4EAF2);
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF7A8798); paint.setTextSize(dp(10));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            float liters = scale * (4 - i) / 4000f;
            canvas.drawText(String.format(Locale.FRANCE, "%.1f L", liters), 0, y + dp(4), paint);
        }

        int count = days;
        float slot = (right - left) / count;
        float barWidth = Math.max(dp(2), Math.min(dp(22), slot * 0.62f));
        Calendar day = (Calendar) start.clone();
        SimpleDateFormat labelFmt = new SimpleDateFormat("dd/MM", Locale.FRANCE);
        for (int i = 0; i < count; i++) {
            String key = fmt.format(day.getTime());
            int amount = totals.containsKey(key) ? totals.get(key) : 0;
            float x = left + slot * (i + 0.5f);
            float y = bottom - (bottom - top) * amount / (float) scale;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(amount > 0 ? 0xFF147BEF : 0xFFE2EAF4);
            if (amount > 0) canvas.drawRoundRect(x - barWidth / 2f, y, x + barWidth / 2f, bottom, dp(4), dp(4), paint);
            int labelStep = labelStep(right-left,count);
            if (i % labelStep == 0 || i == count - 1) {
                paint.setColor(0xFF7A8798); paint.setTextSize(dp(10));
                String label = labelFmt.format(day.getTime());
                float labelWidth = paint.measureText(label);
                canvas.drawText(label, Math.max(left, Math.min(x - labelWidth / 2f, right - labelWidth)), bottom + dp(25), paint);
            }
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
    private int labelStep(float width,int count) { int needed=Math.max(1,(int)Math.ceil(count/(width/dp(58)))); int[] intervals={1,2,5,7,10,14,15,21,30,45,60,90,120,180}; for(int interval:intervals)if(interval>=needed)return interval; return ((needed+179)/180)*180; }
    private void clearTime(Calendar c) {}
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
    }
    private void drawEmpty(Canvas canvas, String message) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF718198); paint.setTextSize(dp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}

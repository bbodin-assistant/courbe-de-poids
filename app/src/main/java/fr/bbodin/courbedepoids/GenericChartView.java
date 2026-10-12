package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GenericChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.GenericEvent> events = new ArrayList<>();
    private String typeId = "weight", fieldId = "weight_kg", aggregation = "last";
    private int days = 7, endOffsetDays;

    public GenericChartView(Context c) { super(c); }
    public GenericChartView(Context c, AttributeSet a) { super(c, a); }
    public GenericChartView(Context c, AttributeSet a, int style) { super(c, a, style); }

    public void setData(List<WeightDatabase.GenericEvent> value, String type, String field, String aggregate, int period, int offsetDays) {
        events = value == null ? new ArrayList<>() : new ArrayList<>(value);
        typeId = type == null ? "" : type;
        fieldId = field == null ? "" : field;
        aggregation = "sum".equals(aggregate) ? "sum" : "last";
        days = Math.max(1, period);
        endOffsetDays = offsetDays;
        invalidate();
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Calendar end = Calendar.getInstance(); clearTime(end); end.add(Calendar.DAY_OF_YEAR, -endOffsetDays);
        Calendar start = (Calendar) end.clone(); start.add(Calendar.DAY_OF_YEAR, -(days - 1));
        SimpleDateFormat key = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String startKey = key.format(start.getTime()), endKey = key.format(end.getTime());
        Map<String, Double> totals = new HashMap<>();
        Map<String, Long> latestTimes = new HashMap<>();
        for (WeightDatabase.GenericEvent event : events) {
            if (!typeId.equals(event.typeId) || event.date.compareTo(startKey) < 0 || event.date.compareTo(endKey) > 0) continue;
            try {
                Object raw = new JSONObject(event.payload).opt(fieldId);
                if (!(raw instanceof Number)) continue;
                double value = ((Number) raw).doubleValue();
                if (!Double.isFinite(value)) continue;
                if ("sum".equals(aggregation)) totals.put(event.date, totals.getOrDefault(event.date, 0d) + value);
                else if (!latestTimes.containsKey(event.date) || event.createdAt >= latestTimes.get(event.date)) {
                    totals.put(event.date, value);
                    latestTimes.put(event.date, event.createdAt);
                }
            } catch (Exception ignored) { }
        }

        float left = dp(43), right = getWidth() - dp(8), top = dp(20), bottom = getHeight() - dp(34);
        if (right <= left || bottom <= top) return;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double value : totals.values()) { min = Math.min(min, value); max = Math.max(max, value); }
        if (totals.isEmpty()) { min = 0; max = 1; }
        double pad = totals.isEmpty() ? 0.25 : Math.max(0.5, (max - min) * 0.18);
        min = Math.floor((min - pad) * 2) / 2d;
        max = Math.ceil((max + pad) * 2) / 2d;
        if (max <= min) max = min + 1;

        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0xFFE4EAF2);
        for (int i = 0; i <= 4; i++) canvas.drawLine(left, top + (bottom - top) * i / 4f, right, top + (bottom - top) * i / 4f, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF7A8798); paint.setTextSize(dp(10)); paint.setTypeface(Typeface.DEFAULT);
        for (int i = 0; i <= 4; i++) {
            double value = max - (max - min) * i / 4d;
            canvas.drawText(String.format(Locale.FRANCE, "%.1f", value), 0, top + (bottom - top) * i / 4f + dp(4), paint);
        }

        Path line = new Path();
        List<Float> xs = new ArrayList<>(), ys = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Calendar date = (Calendar) start.clone(); date.add(Calendar.DAY_OF_YEAR, i);
            String dateKey = key.format(date.getTime());
            Double value = totals.get(dateKey);
            float x = left + (i + 0.5f) * (right - left) / days;
            if (value != null) {
                float y = (float) (bottom - (value - min) / (max - min) * (bottom - top));
                xs.add(x); ys.add(y);
            }
        }
        if (xs.isEmpty()) {
            float mid = (top + bottom) / 2f;
            line.moveTo(left, mid); line.cubicTo(left + (right-left)*0.3f, mid-dp(8), left+(right-left)*0.7f, mid+dp(8), right, mid);
            paint.setColor(0xFF94A3B8); paint.setStrokeWidth(dp(2.5f)); paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(6), dp(5)}, 0)); canvas.drawPath(line, paint); paint.setPathEffect(null);
            paint.setStyle(Paint.Style.FILL); paint.setTextSize(dp(12)); paint.setTextAlign(Paint.Align.CENTER); paint.setColor(0xFF718198);
            canvas.drawText("Aucune donnée sur cette période", getWidth()/2f, bottom+dp(23), paint); paint.setTextAlign(Paint.Align.LEFT);
        } else {
            line.moveTo(left, ys.get(0));
            if (xs.size() == 1) line.lineTo(right, ys.get(0));
            else {
                for (int i = 1; i < xs.size(); i++) {
                    float mid = (xs.get(i-1) + xs.get(i)) / 2f;
                    line.cubicTo(mid, ys.get(i-1), mid, ys.get(i), xs.get(i), ys.get(i));
                }
            }
            canvas.save(); canvas.clipRect(left, top, right, bottom);
            Path area = new Path(line); area.lineTo(xs.get(xs.size()-1), bottom); area.lineTo(xs.get(0), bottom); area.close();
            paint.setStyle(Paint.Style.FILL); paint.setShader(new android.graphics.LinearGradient(0, top, 0, bottom, 0x6673B7FF, 0x0873B7FF, android.graphics.Shader.TileMode.CLAMP)); canvas.drawPath(area, paint); paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE); paint.setColor(0xFF147BEF); paint.setStrokeWidth(dp(3.5f)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); canvas.drawPath(line, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF147BEF);
            for (int i = 0; i < xs.size(); i++) canvas.drawCircle(xs.get(i), ys.get(i), dp(3.5f), paint);
            canvas.restore();
        }

        int step = Math.max(1, (int) Math.ceil(days / Math.max(1f, (right-left) / dp(58))));
        SimpleDateFormat label = new SimpleDateFormat(days > 90 ? "MMM yy" : "dd/MM", Locale.FRANCE);
        for (int i = 0; i < days; i += step) {
            Calendar date = (Calendar) start.clone(); date.add(Calendar.DAY_OF_YEAR, i);
            String text = label.format(date.getTime()); paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF7A8798); paint.setTextSize(dp(10));
            float width = paint.measureText(text), x = left + (i + 0.5f) * (right-left) / days;
            canvas.drawText(text, Math.max(left, Math.min(x-width/2f, right-width)), bottom+dp(17), paint);
        }
    }

    private void clearTime(Calendar c) { c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); }
}

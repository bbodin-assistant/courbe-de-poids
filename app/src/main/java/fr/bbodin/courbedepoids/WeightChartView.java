package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WeightChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.Measurement> data;

    public WeightChartView(Context context) { super(context); }
    public WeightChartView(Context context, AttributeSet attrs) { super(context, attrs); }
    public WeightChartView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    public void setData(List<WeightDatabase.Measurement> data) {
        this.data = data;
        invalidate();
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0x00FFFFFF);
        if (data == null || data.isEmpty()) {
            drawEmpty(canvas, "Ajoutez votre première mesure");
            return;
        }
        if (data.size() == 1) {
            drawEmpty(canvas, "Ajoutez une deuxième mesure pour voir l'évolution");
            return;
        }

        final float left = dp(38);
        final float right = getWidth() - dp(8);
        final float top = dp(16);
        final float bottom = getHeight() - dp(38);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (WeightDatabase.Measurement m : data) {
            min = Math.min(min, m.weight);
            max = Math.max(max, m.weight);
        }
        double range = Math.max(1.0, max - min);
        min = Math.floor((min - range * 0.15) * 2) / 2.0;
        max = Math.ceil((max + range * 0.15) * 2) / 2.0;
        if (max <= min) max = min + 1;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xFFE7EEF7);
        paint.setStrokeCap(Paint.Cap.BUTT);
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(10));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(0xFF718198);
        for (int i = 0; i <= 4; i++) {
            double value = max - (max - min) * i / 4d;
            float y = top + (bottom - top) * i / 4f;
            canvas.drawText(String.format(Locale.FRANCE, "%.1f", value), 0, y + dp(4), paint);
        }

        float slot = (right - left) / data.size();
        float barWidth = Math.min(dp(24), slot * 0.58f);
        for (int i = 0; i < data.size(); i++) {
            WeightDatabase.Measurement m = data.get(i);
            float centerX = left + slot * (i + 0.5f);
            float y = (float) (bottom - (m.weight - min) / (max - min) * (bottom - top));
            RectF bar = new RectF(centerX - barWidth / 2f, y, centerX + barWidth / 2f, bottom);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == data.size() - 1 ? 0xFF147BEF : 0xFF9BCBFF);
            canvas.drawRoundRect(bar, dp(5), dp(5), paint);
        }

        paint.setColor(0xFF718198);
        paint.setTextSize(dp(10));
        int labelStep = Math.max(1, (int) Math.ceil(data.size() / 5.0));
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat output = new SimpleDateFormat("dd/MM", Locale.FRANCE);
        for (int i = 0; i < data.size(); i += labelStep) {
            float x = left + slot * (i + 0.5f);
            String label = data.get(i).date;
            try {
                java.util.Date parsed = input.parse(data.get(i).date);
                if (parsed != null) label = output.format(parsed);
            } catch (Exception ignored) {}
            float textWidth = paint.measureText(label);
            canvas.drawText(label, Math.max(left, Math.min(x - textWidth / 2f, right - textWidth)),
                    getHeight() - dp(10), paint);
        }
        int last = data.size() - 1;
        if (last % labelStep != 0) {
            String label = data.get(last).date;
            try {
                java.util.Date parsed = input.parse(data.get(last).date);
                if (parsed != null) label = output.format(parsed);
            } catch (Exception ignored) {}
            float textWidth = paint.measureText(label);
            canvas.drawText(label, right - textWidth, getHeight() - dp(10), paint);
        }
    }

    private void drawEmpty(Canvas canvas, String message) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF718198);
        paint.setTextSize(dp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
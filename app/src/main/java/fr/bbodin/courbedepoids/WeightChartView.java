package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFFFFFFF);

        if (data == null || data.isEmpty()) {
            drawEmpty(canvas, "Ajoutez votre première mesure");
            return;
        }
        if (data.size() == 1) {
            drawEmpty(canvas, "Ajoutez une deuxième mesure pour voir l'évolution");
            return;
        }

        final float left = 58f;
        final float right = getWidth() - 12f;
        final float top = 18f;
        final float bottom = getHeight() - 52f;

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (WeightDatabase.Measurement m : data) {
            min = Math.min(min, m.weight);
            max = Math.max(max, m.weight);
        }
        double range = Math.max(1.0, max - min);
        min -= range * 0.12;
        max += range * 0.12;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0xFFE2E8F0);
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(11f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(0xFF64748B);
        for (int i = 0; i <= 4; i++) {
            double value = max - (max - min) * i / 4d;
            float y = top + (bottom - top) * i / 4f;
            canvas.drawText(String.format(Locale.FRANCE, "%.1f", value), 4, y + 4, paint);
        }

        Path path = new Path();
        for (int i = 0; i < data.size(); i++) {
            WeightDatabase.Measurement m = data.get(i);
            float x = left + (right - left) * i / (float) (data.size() - 1);
            float y = (float) (bottom - (m.weight - min) / (max - min) * (bottom - top));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }

        paint.setColor(0xFF4F46E5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < data.size(); i++) {
            WeightDatabase.Measurement m = data.get(i);
            float x = left + (right - left) * i / (float) (data.size() - 1);
            float y = (float) (bottom - (m.weight - min) / (max - min) * (bottom - top));
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, 5.5f, paint);
            paint.setColor(0xFF4F46E5);
            canvas.drawCircle(x, y, 3.5f, paint);
        }

        paint.setColor(0xFF64748B);
        paint.setTextSize(10.5f);
        int labelStep = Math.max(1, (int) Math.ceil(data.size() / 4.0));
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat output = new SimpleDateFormat("dd/MM", Locale.FRANCE);
        for (int i = 0; i < data.size(); i += labelStep) {
            float x = left + (right - left) * i / (float) (data.size() - 1);
            String label = data.get(i).date;
            try { label = output.format(input.parse(data.get(i).date)); } catch (Exception ignored) {}
            float textWidth = paint.measureText(label);
            float drawX = Math.max(left, Math.min(x - textWidth / 2f, right - textWidth));
            canvas.drawText(label, drawX, getHeight() - 16f, paint);
        }
        int last = data.size() - 1;
        if (last % labelStep != 0) {
            float x = right;
            String label = data.get(last).date;
            try { label = output.format(input.parse(data.get(last).date)); } catch (Exception ignored) {}
            float textWidth = paint.measureText(label);
            canvas.drawText(label, x - textWidth, getHeight() - 16f, paint);
        }
    }

    private void drawEmpty(Canvas canvas, String message) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF64748B);
        paint.setTextSize(13f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}

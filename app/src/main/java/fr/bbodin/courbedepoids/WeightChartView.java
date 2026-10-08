package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

public class WeightChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.Measurement> data;

    public WeightChartView(Context context) { super(context); paint.setStrokeWidth(4f); }

    public WeightChartView(Context context, AttributeSet attrs) { super(context, attrs); paint.setStrokeWidth(4f); }

    public WeightChartView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); paint.setStrokeWidth(4f); }

    public void setData(List<WeightDatabase.Measurement> data) { this.data = data; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFF7F7F7);
        if (data == null || data.size() < 2) {
            paint.setTextSize(36);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("Ajoutez au moins 2 mesures", 30, getHeight() / 2f, paint);
            return;
        }
        float left=70, right=getWidth()-30, top=35, bottom=getHeight()-55;
        double min=Double.MAX_VALUE, max=-Double.MAX_VALUE;
        for (WeightDatabase.Measurement m:data){ min=Math.min(min,m.weight); max=Math.max(max,m.weight); }
        double range=Math.max(1.0,max-min);
        min-=range*.1; max+=range*.1;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawLine(left,bottom,right,bottom,paint);
        canvas.drawLine(left,top,left,bottom,paint);
        Path path=new Path();
        for(int i=0;i<data.size();i++){
            WeightDatabase.Measurement m=data.get(data.size()-1-i);
            float x=left+(right-left)*(i/(float)Math.max(1,data.size()-1));
            float y=(float)(bottom-(m.weight-min)/(max-min)*(bottom-top));
            if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x,y,6,paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(28);
        canvas.drawText(String.format(java.util.Locale.FRANCE,"%.1f kg",max),5,top+10,paint);
        canvas.drawText(String.format(java.util.Locale.FRANCE,"%.1f kg",min),5,bottom,paint);
    }
}

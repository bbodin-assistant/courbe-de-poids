package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WeightChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.Measurement> data;
    public WeightChartView(Context c) { super(c); }
    public WeightChartView(Context c, AttributeSet a) { super(c, a); }
    public WeightChartView(Context c, AttributeSet a, int d) { super(c, a, d); }
    public void setData(List<WeightDatabase.Measurement> value) { data = value; invalidate(); }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) { drawEmpty(canvas, "Aucune mesure sur cette période"); return; }
        float left = dp(42), right = getWidth() - dp(10), top = dp(18), bottom = getHeight() - dp(34);
        if (right <= left || bottom <= top) return;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (WeightDatabase.Measurement m : data) { min = Math.min(min, m.weight); max = Math.max(max, m.weight); }
        double pad = Math.max(0.5, (max - min) * 0.22);
        min = Math.floor((min - pad) * 2) / 2.0; max = Math.ceil((max + pad) * 2) / 2.0;
        if (max <= min) max = min + 1;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0xFFE4EAF2);
        for (int i=0;i<=4;i++) { float y=top+(bottom-top)*i/4f; canvas.drawLine(left,y,right,y,paint); }
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF7A8798); paint.setTextSize(dp(10));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int i=0;i<=4;i++) {
            double v=max-(max-min)*i/4d; float y=top+(bottom-top)*i/4f;
            canvas.drawText(String.format(Locale.FRANCE,"%.1f",v),0,y+dp(4),paint);
        }
        float step=data.size()==1?0:(right-left)/(data.size()-1);
        float[] xs=new float[data.size()], ys=new float[data.size()];
        for(int i=0;i<data.size();i++){ xs[i]=data.size()==1?(left+right)/2f:left+i*step; ys[i]=(float)(bottom-(data.get(i).weight-min)/(max-min)*(bottom-top)); }
        Path line=new Path(); line.moveTo(xs[0],ys[0]);
        for(int i=1;i<data.size();i++){ float mid=(xs[i-1]+xs[i])/2f; line.cubicTo(mid,ys[i-1],mid,ys[i],xs[i],ys[i]); }
        Path area=new Path(line); area.lineTo(xs[xs.length-1],bottom); area.lineTo(xs[0],bottom); area.close();
        paint.setStyle(Paint.Style.FILL); paint.setShader(new LinearGradient(0,top,0,bottom,0x6673B7FF,0x0873B7FF,Shader.TileMode.CLAMP)); canvas.drawPath(area,paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(0xFF147BEF); paint.setStrokeWidth(dp(3.5f)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); canvas.drawPath(line,paint);
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<data.size();i++){ paint.setColor(0xFFFFFFFF); canvas.drawCircle(xs[i],ys[i],dp(6.5f),paint); paint.setColor(0xFF147BEF); canvas.drawCircle(xs[i],ys[i],dp(4.5f),paint); }
        paint.setColor(0xFF7A8798); paint.setTextSize(dp(10));
        int labelStep=Math.max(1,(int)Math.ceil(data.size()/5.0));
        SimpleDateFormat input=new SimpleDateFormat("yyyy-MM-dd",Locale.US), output=new SimpleDateFormat("dd/MM",Locale.FRANCE);
        for(int i=0;i<data.size();i+=labelStep) drawDate(canvas,data.get(i).date,xs[i],left,right,bottom,input,output);
        int last=data.size()-1; if(last%labelStep!=0) drawDate(canvas,data.get(last).date,xs[last],left,right,bottom,input,output);
    }
    private void drawDate(Canvas c,String raw,float x,float left,float right,float bottom,SimpleDateFormat in,SimpleDateFormat out) {
        String label=raw; try { java.util.Date d=in.parse(raw); if(d!=null) label=out.format(d); } catch(Exception ignored) {}
        float w=paint.measureText(label); c.drawText(label,Math.max(left,Math.min(x-w/2f,right-w)),bottom+dp(26),paint);
    }
    private void drawEmpty(Canvas c,String message) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF718198); paint.setTextSize(dp(13)); paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(message,getWidth()/2f,getHeight()/2f,paint); paint.setTextAlign(Paint.Align.LEFT);
    }
}
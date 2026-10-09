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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WeightChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.Measurement> data = new ArrayList<>();
    private Calendar rangeStart, rangeEnd;
    public WeightChartView(Context c) { super(c); }
    public WeightChartView(Context c, AttributeSet a) { super(c,a); }
    public WeightChartView(Context c, AttributeSet a, int d) { super(c,a,d); }
    public void setData(List<WeightDatabase.Measurement> value) {
        data=value==null?new ArrayList<>():new ArrayList<>(value);
        Calendar end=Calendar.getInstance(); clearTime(end);
        rangeEnd=(Calendar)end.clone(); rangeStart=(Calendar)end.clone(); rangeStart.add(Calendar.DAY_OF_YEAR,-6);
        invalidate();
    }
    public void setData(List<WeightDatabase.Measurement> value, Calendar start, Calendar end) {
        data=value==null?new ArrayList<>():new ArrayList<>(value);
        rangeStart=start==null?null:(Calendar)start.clone();
        rangeEnd=end==null?null:(Calendar)end.clone();
        invalidate();
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(data.isEmpty()){drawEmpty(canvas,"Aucune mesure sur cette période");return;}
        if(rangeStart==null||rangeEnd==null){Calendar end=Calendar.getInstance();clearTime(end);rangeEnd=(Calendar)end.clone();rangeStart=(Calendar)end.clone();rangeStart.add(Calendar.DAY_OF_YEAR,-6);}
        float left=dp(42),right=getWidth()-dp(10),top=dp(18),bottom=getHeight()-dp(34);
        if(right<=left||bottom<=top)return;
        int startDay=day(ChartRange.format(rangeStart)),endDay=day(ChartRange.format(rangeEnd));
        if(startDay==Integer.MIN_VALUE||endDay<=startDay)return;
        int lastDay=day(data.get(data.size()-1).date);
        int forecastEndDay=lastDay==Integer.MIN_VALUE?endDay:Math.min(endDay,lastDay+ChartRange.PREDICTION_DAYS);
        List<Point> points=new ArrayList<>();
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        int firstVisible=data.size(), lastVisible=-1;
        for(int i=0;i<data.size();i++){
            int d=day(data.get(i).date);
            if(d!=Integer.MIN_VALUE&&d>=startDay&&d<=endDay){firstVisible=Math.min(firstVisible,i);lastVisible=i;}
        }
        int from=firstVisible==data.size()?0:Math.max(0,firstVisible-1);
        int to=lastVisible<0?data.size()-1:Math.min(data.size()-1,lastVisible+1);
        for(int i=from;i<=to;i++){
            WeightDatabase.Measurement m=data.get(i);
            int d=day(m.date);
            if(d==Integer.MIN_VALUE)continue;
            Point p=new Point(d,m.weight,m.date);
            points.add(p);min=Math.min(min,p.weight);max=Math.max(max,p.weight);
        }
        if(points.isEmpty()){drawEmpty(canvas,"Aucune mesure sur cette période");return;}
        Prediction prediction=prediction(data);
        if(prediction!=null&&forecastEndDay>=lastDay){
            double projected=prediction.valueAt(forecastEndDay);
            min=Math.min(min,projected);max=Math.max(max,projected);
        }
        double pad=Math.max(0.5,(max-min)*0.22);
        min=Math.floor((min-pad)*2)/2.0;max=Math.ceil((max+pad)*2)/2.0;if(max<=min)max=min+1;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0xFFE4EAF2);
        for(int i=0;i<=4;i++){float y=top+(bottom-top)*i/4f;canvas.drawLine(left,y,right,y,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF7A8798);paint.setTextSize(dp(10));paint.setTypeface(Typeface.DEFAULT);
        for(int i=0;i<=4;i++){double v=max-(max-min)*i/4d;float y=top+(bottom-top)*i/4f;canvas.drawText(String.format(Locale.FRANCE,"%.1f",v),0,y+dp(4),paint);}
        float scaleX=(right-left)/(endDay-startDay);
        Path line=new Path();boolean started=false;
        for(Point p:points){
            float px=x(p.day,startDay,scaleX,left),py=y(p.weight,min,max,top,bottom);
            if(!started){line.moveTo(px,py);started=true;}else{
                // Cubic interpolation stays continuous when points lie outside the viewport.
                // The canvas clip reveals the correct segment while scrolling.
                Point previous=points.get(points.indexOf(p)-1);
                float prevX=x(previous.day,startDay,scaleX,left),prevY=y(previous.weight,min,max,top,bottom),mid=(prevX+px)/2f;
                line.cubicTo(mid,prevY,mid,py,px,py);
            }
        }
        canvas.save();canvas.clipRect(left,top,right,bottom);
        Path area=new Path(line);
        Point first=points.get(0),last=points.get(points.size()-1);
        area.lineTo(x(last.day,startDay,scaleX,left),bottom);area.lineTo(x(first.day,startDay,scaleX,left),bottom);area.close();
        paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,top,0,bottom,0x6673B7FF,0x0873B7FF,Shader.TileMode.CLAMP));canvas.drawPath(area,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF147BEF);paint.setStrokeWidth(dp(3.5f));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(line,paint);
        // Forecast: least-squares trend of the last eight measurements, projected at most 14 days.
        if(prediction!=null&&forecastEndDay>lastDay){
            Point latest=null;
            for(Point p:points)if(p.day==lastDay)latest=p;
            if(latest==null){
                WeightDatabase.Measurement m=data.get(data.size()-1);
                latest=new Point(lastDay,m.weight,m.date);
            }
            Path future=new Path();future.moveTo(x(lastDay,startDay,scaleX,left),y(prediction.valueAt(lastDay),min,max,top,bottom));
            future.lineTo(x(forecastEndDay,startDay,scaleX,left),y(prediction.valueAt(forecastEndDay),min,max,top,bottom));
            paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF16A34A);paint.setStrokeWidth(dp(2.5f));paint.setStrokeCap(Paint.Cap.ROUND);paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(7),dp(5)},0));canvas.drawPath(future,paint);paint.setPathEffect(null);
        }
        for(Point p:points){
            float px=x(p.day,startDay,scaleX,left);
            if(px<left-dp(7)||px>right+dp(7))continue;
            float py=y(p.weight,min,max,top,bottom);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFFFFF);canvas.drawCircle(px,py,dp(6.5f),paint);paint.setColor(0xFF147BEF);canvas.drawCircle(px,py,dp(4.5f),paint);
        }
        canvas.restore();
        paint.setColor(0xFF7A8798);paint.setTextSize(dp(10));
        SimpleDateFormat input=new SimpleDateFormat("yyyy-MM-dd",Locale.US),output=new SimpleDateFormat("dd/MM",Locale.FRANCE);
        Calendar label=(Calendar)rangeStart.clone();int count=endDay-startDay+1;int step=Math.max(1,(int)Math.ceil(count/5.0));
        for(int i=0;i<count;i+=step){drawDate(canvas,ChartRange.format(label),x(startDay+i,startDay,scaleX,left),left,right,bottom,input,output);label.add(Calendar.DAY_OF_YEAR,step);}
        if((count-1)%step!=0)drawDate(canvas,ChartRange.format(rangeEnd),right,left,right,bottom,input,output);
    }
    private Prediction prediction(List<WeightDatabase.Measurement> values){
        int n=Math.min(8,values.size());if(n<2)return null;
        int from=values.size()-n;double meanX=0,meanY=0;
        int[] days=new int[n];double[] weights=new double[n];
        for(int i=0;i<n;i++){days[i]=day(values.get(from+i).date);weights[i]=values.get(from+i).weight;if(days[i]==Integer.MIN_VALUE)return null;meanX+=days[i];meanY+=weights[i];}
        meanX/=n;meanY/=n;double numerator=0,denominator=0;
        for(int i=0;i<n;i++){double dx=days[i]-meanX;numerator+=dx*(weights[i]-meanY);denominator+=dx*dx;}
        if(denominator==0)return null;double slope=numerator/denominator;
        return new Prediction(days[n-1],weights[n-1],slope);
    }
    private int day(String value){return ChartRange.dayNumber(value);}
    private float x(int day,int startDay,float scale,float left){return left+(day-startDay)*scale;}
    private float y(double value,double min,double max,float top,float bottom){return(float)(bottom-(value-min)/(max-min)*(bottom-top));}
    private void drawDate(Canvas c,String raw,float x,float left,float right,float bottom,SimpleDateFormat in,SimpleDateFormat out){
        String label=raw;try{java.util.Date d=in.parse(raw);if(d!=null)label=out.format(d);}catch(Exception ignored){}
        float w=paint.measureText(label);c.drawText(label,Math.max(left,Math.min(x-w/2f,right-w)),bottom+dp(26),paint);
    }
    private void drawEmpty(Canvas c,String message){paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF718198);paint.setTextSize(dp(13));paint.setTextAlign(Paint.Align.CENTER);c.drawText(message,getWidth()/2f,getHeight()/2f,paint);paint.setTextAlign(Paint.Align.LEFT);}
    private void clearTime(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
    private static final class Point{final int day;final double weight;final String date;Point(int d,double w,String s){day=d;weight=w;date=s;}}
    private static final class Prediction{final int baseDay;final double baseWeight,slope;Prediction(int d,double w,double s){baseDay=d;baseWeight=w;slope=s;}double valueAt(int day){return baseWeight+slope*(day-baseDay);}}
}

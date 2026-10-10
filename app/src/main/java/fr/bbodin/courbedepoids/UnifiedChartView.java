package fr.bbodin.courbedepoids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UnifiedChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeightDatabase.Measurement> weights = new ArrayList<>();
    private List<WeightDatabase.WaterEvent> water = new ArrayList<>();
    private List<WeightDatabase.SportEvent> sports = new ArrayList<>();
    private int days = 7;
    private int endOffsetDays = 0;

    public UnifiedChartView(Context c) { super(c); }
    public UnifiedChartView(Context c, AttributeSet a) { super(c, a); }
    public UnifiedChartView(Context c, AttributeSet a, int style) { super(c, a, style); }
    public void setData(List<WeightDatabase.Measurement> w, List<WeightDatabase.WaterEvent> a, List<WeightDatabase.SportEvent> s, int period, int offsetDays) {
        weights = w == null ? new ArrayList<>() : new ArrayList<>(w);
        water = a == null ? new ArrayList<>() : new ArrayList<>(a);
        sports = s == null ? new ArrayList<>() : new ArrayList<>(s);
        days = Math.max(3, period);
        endOffsetDays = offsetDays;
        invalidate();
    }
    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Calendar today = Calendar.getInstance(); clearTime(today);
        Calendar end = (Calendar) today.clone(); end.add(Calendar.DAY_OF_YEAR, -endOffsetDays);
        Calendar start = (Calendar) end.clone(); start.add(Calendar.DAY_OF_YEAR, -(days - 1));
        SimpleDateFormat key = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Map<String,Integer> indexes = new HashMap<>();
        Calendar cursor = (Calendar) start.clone();
        for (int i=0;i<days;i++) { indexes.put(key.format(cursor.getTime()),i); cursor.add(Calendar.DAY_OF_YEAR,1); }
        Map<String,Integer> waterByDay = new HashMap<>();
        for (WeightDatabase.WaterEvent e:water) if(indexes.containsKey(e.date)) waterByDay.put(e.date,(waterByDay.containsKey(e.date)?waterByDay.get(e.date):0)+e.amountMl);
        Set<String> sportDays = new HashSet<>();
        for (WeightDatabase.SportEvent e:sports) if(indexes.containsKey(e.date)) sportDays.add(e.date);
        String startKey=key.format(start.getTime()), endKey=key.format(end.getTime());
        WeightDatabase.Measurement before=null, after=null;
        List<WeightDatabase.Measurement> visible=new ArrayList<>();
        for(WeightDatabase.Measurement m:weights){
            if(m.date.compareTo(startKey)<0) before=m;
            else if(m.date.compareTo(endKey)>0){if(after==null)after=m;}
            else visible.add(m);
        }
        List<WeightDatabase.Measurement> data=new ArrayList<>();
        if(before!=null)data.add(before);
        data.addAll(visible);
        if(after!=null)data.add(after);

        float left=dp(43),right=getWidth()-dp(8),top=dp(20),bottom=getHeight()-dp(62);
        if(right<=left||bottom<=top)return;
        if(data.isEmpty()){drawPlaceholderCurve(canvas);return;}
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for(WeightDatabase.Measurement m:data){min=Math.min(min,m.weight);max=Math.max(max,m.weight);}
        double pad=Math.max(0.5,(max-min)*0.2);min=Math.floor((min-pad)*2)/2.0;max=Math.ceil((max+pad)*2)/2.0;if(max<=min)max=min+1;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0xFFE4EAF2);
        for(int i=0;i<=4;i++){float y=top+(bottom-top)*i/4f;canvas.drawLine(left,y,right,y,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF7A8798);paint.setTextSize(dp(10));paint.setTypeface(Typeface.DEFAULT);
        for(int i=0;i<=4;i++){double value=max-(max-min)*i/4.0;float y=top+(bottom-top)*i/4f;canvas.drawText(String.format(Locale.FRANCE,"%.1f",value),0,y+dp(4),paint);}

        float[] xs=new float[data.size()],ys=new float[data.size()];int[] dayNumbers=new int[data.size()];
        for(int i=0;i<data.size();i++){
            WeightDatabase.Measurement m=data.get(i);Integer ix=indexes.get(m.date);
            int measurementDay=ChartRange.dayNumber(m.date);
            int startDayNumber=ChartRange.dayNumber(startKey);
            dayNumbers[i]=measurementDay!=Integer.MIN_VALUE&&startDayNumber!=Integer.MIN_VALUE?measurementDay-startDayNumber:(ix!=null?ix:0);
            xs[i]=x(dayNumbers[i],left,right);ys[i]=(float)(bottom-(m.weight-min)/(max-min)*(bottom-top));
        }
        Path line=new Path();
        if(xs.length==1){line.moveTo(left,ys[0]);line.lineTo(right,ys[0]);}
        else{line.moveTo(xs[0],ys[0]);for(int i=1;i<xs.length;i++)line.lineTo(xs[i],ys[i]);}
        Path area=new Path(line);area.lineTo(xs[xs.length-1],bottom);area.lineTo(xs[0],bottom);area.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(0x1873B7FF);canvas.drawPath(area,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF147BEF);paint.setStrokeWidth(dp(3));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(line,paint);
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<xs.length;i++){
            if(data.get(i).date.compareTo(startKey)<0||data.get(i).date.compareTo(endKey)>0)continue;
            paint.setColor(0xFFFFFFFF);canvas.drawCircle(xs[i],ys[i],dp(5.5f),paint);
            paint.setColor(0xFF147BEF);canvas.drawCircle(xs[i],ys[i],dp(3.5f),paint);
        }
        for(String date:sportDays){Integer ix=indexes.get(date);if(ix!=null)drawSport(canvas,x(ix,left,right),Math.max(top+dp(13),interpolate(ix,dayNumbers,ys)-dp(15)));}

        int step=days<=7?1:days<=30?5:days<=90?15:60;
        Calendar labelDay=(Calendar)start.clone();
        SimpleDateFormat labelFormat=new SimpleDateFormat(days>90?"MMM yy":"dd/MM",Locale.FRANCE);
        for(int i=0;i<days;i++){
            if(i%step!=0&&i!=days-1){labelDay.add(Calendar.DAY_OF_YEAR,1);continue;}
            String date=key.format(labelDay.getTime());float xpos=x(i,left,right);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF7A8798);paint.setTextSize(dp(days>90?9:9.5f));
            String label=labelFormat.format(labelDay.getTime());float width=paint.measureText(label);
            canvas.drawText(label,Math.max(left,Math.min(xpos-width/2f,right-width)),bottom+dp(17),paint);
            Integer amount=waterByDay.get(date);String amountLabel=amount==null?"—":String.format(Locale.FRANCE,"%.1fL",amount/1000.0);
            paint.setColor(amount==null?0xFFB1BBC8:0xFF0891B2);paint.setTextSize(dp(9));float amountWidth=paint.measureText(amountLabel);
            canvas.drawText(amountLabel,Math.max(left,Math.min(xpos-amountWidth/2f,right-amountWidth)),bottom+dp(34),paint);
            labelDay.add(Calendar.DAY_OF_YEAR,1);
        }
    }
    private void drawPlaceholderCurve(Canvas c){
        float left=dp(43),right=getWidth()-dp(8),top=dp(20),bottom=getHeight()-dp(62);
        if(right<=left||bottom<=top)return;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0xFFE4EAF2);
        for(int i=0;i<=4;i++)c.drawLine(left,top+(bottom-top)*i/4f,right,top+(bottom-top)*i/4f,paint);
        float mid=(top+bottom)/2f;Path p=new Path();p.moveTo(left,mid+dp(9));
        p.cubicTo(left+(right-left)*.25f,mid+dp(9),left+(right-left)*.25f,mid-dp(9),left+(right-left)*.5f,mid-dp(9));
        p.cubicTo(left+(right-left)*.75f,mid-dp(9),left+(right-left)*.75f,mid+dp(9),right,mid+dp(9));
        paint.setColor(0xFF94A3B8);paint.setStrokeWidth(dp(2.5f));paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(6),dp(5)},0));c.drawPath(p,paint);paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF718198);paint.setTextSize(dp(12));paint.setTextAlign(Paint.Align.CENTER);
        c.drawText("Ajoutez une mesure pour tracer votre évolution",getWidth()/2f,bottom+dp(45),paint);paint.setTextAlign(Paint.Align.LEFT);
    }
    private float x(int index,float left,float right){return left+(index+0.5f)*(right-left)/days;}
    private float interpolate(int index,int[] indexes,float[] ys){
        if(index<=indexes[0])return ys[0];int last=indexes.length-1;if(index>=indexes[last])return ys[last];
        for(int i=1;i<indexes.length;i++)if(index<=indexes[i]){int span=indexes[i]-indexes[i-1];float fraction=span<=0?1:(index-indexes[i-1])/(float)span;return ys[i-1]+(ys[i]-ys[i-1])*fraction;}
        return ys[last];
    }
    private void drawSport(Canvas c,float x,float y){
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFFFFF);c.drawCircle(x,y,dp(10),paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.8f));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xFF147BEF);
        c.drawCircle(x,y-dp(4.5f),dp(2),paint);c.drawLine(x,y-dp(2),x,y+dp(3),paint);
        c.drawLine(x,y,x-dp(4),y+dp(1),paint);c.drawLine(x,y,x+dp(4),y-dp(2),paint);
        c.drawLine(x,y+dp(3),x-dp(3),y+dp(7),paint);c.drawLine(x,y+dp(3),x+dp(4),y+dp(6),paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void clearTime(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
    private void empty(Canvas c,String message){paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF718198);paint.setTextSize(dp(13));paint.setTextAlign(Paint.Align.CENTER);c.drawText(message,getWidth()/2f,getHeight()/2f,paint);paint.setTextAlign(Paint.Align.LEFT);}
}

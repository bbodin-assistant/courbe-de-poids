package fr.bbodin.courbedepoids;
import android.view.MotionEvent;
import android.view.View;

public final class ChartRangeGestureListener implements View.OnTouchListener {
    public interface Listener { void onRangeChanged(int days, int offset); }
    private final View v;
    private final Listener l;
    private int days, offset;
    private int minOffset = ChartRange.MIN_END_OFFSET_DAYS, maxOffset = ChartRange.MAX_END_OFFSET_DAYS;
    private float x, y, span, anchor, rem;
    private boolean pinch;
    public ChartRangeGestureListener(View v, int d, int o, Listener l) { this.v=v; days=ChartRange.clampVisibleDays(d); offset=o; this.l=l; }
    public void setOffsetBounds(int min, int max) { minOffset=Math.min(min,max); maxOffset=Math.max(min,max); offset=clampOffset(offset,days); }
    public void setRange(int d,int o) { days=ChartRange.clampVisibleDays(d); offset=clampOffset(o,days); }
    private int clampOffset(int value,int visibleDays) { return ChartRange.clampOffsetToData(value,visibleDays,minOffset,maxOffset); }
    private void send(int d,int o) {
        d=ChartRange.clampVisibleDays(d); o=clampOffset(o,d);
        if(d==days&&o==offset)return;
        days=d;offset=o;l.onRangeChanged(d,o);
    }
    private void block(){if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);}
    @Override public boolean onTouch(View a,MotionEvent e){
        int k=e.getActionMasked();
        if(k==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();pinch=false;rem=0;return true;}
        if(k==MotionEvent.ACTION_POINTER_DOWN&&e.getPointerCount()>1){pinch=true;block();span=dist(e);float f=Math.max(0,Math.min(1,mid(e)/Math.max(1,a.getWidth())));anchor=-offset-days+1+f*(days-1);return true;}
        if(k==MotionEvent.ACTION_MOVE&&e.getPointerCount()>1){float s=dist(e);if(span>0&&s>0){int n=Math.max(3,Math.min(3650,Math.round(days*span/s)));float f=Math.max(0,Math.min(1,mid(e)/Math.max(1,a.getWidth())));span=s;send(n,Math.round(-anchor-(1-f)*(n-1)));}block();return true;}
        if(k==MotionEvent.ACTION_MOVE&&!pinch){float nx=e.getX(),ny=e.getY(),dx=nx-x;if(Math.abs(dx)>Math.abs(ny-y)*1.15f){block();rem+=dx*days/Math.max(1,a.getWidth());int shift=Math.round(rem);if(shift!=0){rem-=shift;send(days,offset+shift);}}x=nx;y=ny;return true;}
        if(k==MotionEvent.ACTION_UP||k==MotionEvent.ACTION_CANCEL){pinch=false;rem=0;if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);}return true;
    }
    private float dist(MotionEvent e){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);return(float)Math.sqrt(dx*dx+dy*dy);}
    private float mid(MotionEvent e){return(e.getX(0)+e.getX(1))/2;}
}

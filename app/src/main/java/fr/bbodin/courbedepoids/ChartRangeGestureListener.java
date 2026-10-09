package fr.bbodin.courbedepoids;
import android.view.MotionEvent;import android.view.View;
public final class ChartRangeGestureListener implements View.OnTouchListener{
 public interface Listener{void onRangeChanged(int days,int offset);}
 private final View v;private final Listener l;private int days,offset;private float x,y,span,anchor;private boolean pinch;
 public ChartRangeGestureListener(View v,int d,int o,Listener l){this.v=v;days=d;offset=o;this.l=l;}
 public void setRange(int d,int o){days=Math.max(3,Math.min(3650,d));offset=Math.max(-3650,Math.min(36500,o));}
 private void send(int d,int o){d=Math.max(3,Math.min(3650,d));o=Math.max(-3650,Math.min(36500,o));if(d==days&&o==offset)return;days=d;offset=o;l.onRangeChanged(d,o);}
 private void block(){if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);}
 @Override public boolean onTouch(View a,MotionEvent e){int k=e.getActionMasked();
 if(k==0){x=e.getX();y=e.getY();pinch=false;return true;}
 if(k==5&&e.getPointerCount()>1){pinch=true;block();span=dist(e);float f=mid(e)/Math.max(1,a.getWidth());anchor=-offset-days+1+f*(days-1);return true;}
 if(k==2&&e.getPointerCount()>1){float s=dist(e);if(span>0&&s>0){int n=Math.max(3,Math.min(3650,Math.round(days*span/s)));float f=Math.max(0,Math.min(1,mid(e)/Math.max(1,a.getWidth())));span=s;send(n,Math.round(-anchor-(1-f)*(n-1)));}block();return true;}
 if(k==2&&!pinch){float nx=e.getX(),ny=e.getY(),dx=x-nx;if(Math.abs(dx)>Math.abs(ny-y)*1.15f){block();int shift=Math.round(dx*days/Math.max(1,a.getWidth()));if(shift!=0)send(days,offset+shift);}x=nx;y=ny;return true;}
 if(k==1||k==3){pinch=false;if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);}return true;}
 private float dist(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return(float)Math.sqrt(x*x+y*y);}
 private float mid(MotionEvent e){return(e.getX(0)+e.getX(1))/2;}
}
package com.rose.android.util.a;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Selection;
import android.view.MotionEvent;
import com.rose.android.widget.CodeEditor;
import android.graphics.RectF;
import com.rose.android.Debug;

//Created By Rose on 2019/7/27

//Helper class for CodeEditor
//This class handles the selection actions when
//we selected text.And it also draw the 
//selection modify handles
//You can create subclass of it to make custom style
public class SelectionController
{
	protected CodeEditor target;
	
	private Handle left;
	private Handle right;
	
	public SelectionController(CodeEditor ce){
		target = ce;
		left = new Handle();
		right = new Handle();
	}
	
	public Editable getText(){
		return target.getEditableText();
	}
	
	public int getEnd(){
		return Selection.getSelectionEnd(getText());
	}
	
	public int getStart(){
		return Selection.getSelectionStart(getText());
	}
	
	public void setStart(int st){
		Selection.setSelection(getText(),st,getEnd());
		dirty();
	}
	
	public void setEnd(int ed){
		Selection.setSelection(getText(),getStart(),ed);
		dirty();
	}
	
	public void dirty(){
		target.invalidate();
	}
	
	static boolean isInBounds(float x,float y,RectF rect){
		return (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom);
	}
	
	static float[] realativePosition(float x,float y,RectF rect){
		return new float[]{x - rect.left,y - rect.top};
	}
	
	private boolean thumbDown = false;
	private int name = 0;
	private float downX,downY;
	
	public boolean onTouchEvent(MotionEvent event){
		if(getStart() != getEnd()){
			float x = event.getX();
			float y = event.getY();
			switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					downX = x;
					downY = y;
					if(isInBounds(x,y,left.getBounds())){
						name = 1;
						return true;
					}else if(isInBounds(x,y,right.getBounds())){
						name = 2;
						return true;
					}else{
						//Debug.debug("x:"+x+" y:"+y+" bounds"+left.getBounds().toString());
					}
					break;
				case MotionEvent.ACTION_MOVE:
					int newIndex = target.getCharOffsetByThumb(x,target.getLineByThumbY(y - left.getBounds().height()));
					switch(name){
						case 0:
							return false;
						case 1:
							if(newIndex < getEnd()){
								setStart(newIndex);
							}else{
								name = 2;
								Selection.setSelection(getText(),getEnd(),newIndex);
								dirty();
							}
							break;
						case 2:
							if(newIndex > getStart()){
								setEnd(newIndex);
							}else{
								name = 1;
								Selection.setSelection(getText(),newIndex,getStart());
								dirty();
							}
					}
					target.getStates().setModification(true);
					return true;
				case MotionEvent.ACTION_UP:
					thumbDown = false;
					name = 0;
					break;
			}
		}
		return false;
	}
	
	public void onDraw(Canvas canvas){
		{
			int i = getStart();
			int line = target.getLineByIndex(i);
			float top = target.getLineTopOnScreen(line);
			//float bottom = top + target.getStyles().getLineHeight() * 2;
			float xOffset = target.getLastOffset() + target.measureText(target.getLineStart(line),i);
			left.draw(canvas,top,xOffset);
		}
		{
			int i = getEnd();
			int line = target.getLineByIndex(i);
			float top = target.getLineTopOnScreen(line);
			//float bottom = top + target.getStyles().getLineHeight() * 2;
			float xOffset = target.getLastOffset() + target.measureText(target.getLineStart(line),i);
			right.draw(canvas,top,xOffset);
		}
	}
	
	public Handle getLeftHandle(){
		return left;
	}
	
	public Handle getRightHandle(){
		return right;
	}
	
	public class Handle{
		private Paint mPaint;
		private RectF mBounds;
		private float r;
		
		public Handle(){
			mPaint=new Paint();
			mPaint.setColor(target.getStyles().getHandleColor());
			mPaint.setAntiAlias(true);
			mPaint.setTextSize(52.0f);
			r = (mPaint.descent()-mPaint.ascent())/2;
			setBounds(0,0,0,0);
		}
		
		public void setHandleColor(int color){
			mPaint.setColor(color);
		}

		public void draw(Canvas canvas,float top,float centerX){
			float cy = top + target.getStyles().getLineHeight() + r;
			canvas.drawCircle(centerX,cy,r,mPaint);
			setBounds(centerX-r,cy-r,centerX+r,cy+r);
		}
		
		private void setBounds(float left,float top,float right,float bottom){
			mBounds = new RectF(left,top,right,bottom);
		}
		
		public RectF getBounds(){
			return mBounds;
		}
	}
}

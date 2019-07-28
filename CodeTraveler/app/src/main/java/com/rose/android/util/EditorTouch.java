package com.rose.android.util;

//Created By Rose in 2019/7/20

import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import com.rose.android.widget.CodeEditor;
import android.view.GestureDetector.OnContextClickListener;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Selection;
import com.rose.android.Debug;
import java.util.Random;

//Helper class of CodeEditor
//This class handles the touch events of the editor
public class EditorTouch implements OnGestureListener,OnDoubleTapListener,OnScaleGestureListener
{
	private final static String LOG_TAG = "EditorTouchResolver";
	
	//Serve Target
	private CodeEditor editor;
	
	//This will be supported future
	private OverScroller scroller;
	
	//Whether can scroll
	private boolean scrollable;
	
	//Disable/Enable fling
	private boolean dragMode;
	
	//Whether can scale by thumb
	private boolean scaleEnabled;
	
	//Internal state flag
	private boolean scale;
	
	//Max X
	private int scrollMaxX;
	
	//Max Y
	private int scrollMaxY;
	
	//Is the last event a selections modification
	private boolean modification;
	
	public EditorTouch(CodeEditor e){
		this.editor = e;
		this.scroller = new OverScroller(editor.getContext());
		scroller.startScroll(0,0,0,0,0);
		this.scrollable = true;
		this.dragMode = false;
		this.scaleEnabled = true;
		this.scale=false;
		this.modification=false;
	}
	
	//Simple setter
	public void setDrag(boolean dragMode){
		this.dragMode = dragMode;
	}
	
	//Simple getter
	public boolean isDragMode(){
		return this.dragMode;
	}
	
	//Simple setter
	public void setScrollable(boolean scrollable){
		this.scrollable = scrollable;
	}
	
	//Simple getter
	public boolean canScroll(){
		return this.scrollable;
	}
	
	//Simple setter
	public void setScaleEnabled(boolean scale){
		this.scaleEnabled = scale;
	}
	
	//Simple getter
	public boolean canScaleByThumb(){
		return this.scaleEnabled;
	}
	
	//Internal method for CodeEditor
	//Reset the states
	public void resetForNewText(){
		scroller.forceFinished(true);
		scroller.startScroll(0,0,0,0,0);
		this.scrollMaxX = 0;
		this.scrollMaxY = 0;
		this.modification=false;
	}
	
	//Show input method for the editor
	public boolean showSoftInputForEditor(){
		if(!editor.hasFocus()){
			editor.requestFocus();
			if(!editor.hasFocus()){
				editor.requestFocusFromTouch();
			}
		}
		boolean r = editor.hasFocus();
		if(r){
			r = r && ((InputMethodManager)editor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editor,0);
		}
		return r;
	}
	
	//Get the scroller
	public OverScroller getScroller(){
		return scroller;
	}
	
	//Internal method
	//Limit the scroll bounds
	public void setScrollMaxX(int maxX){
		if(maxX < 0){
			maxX = 0;
		}
		//We do not know the real right edge of
		//X axis.We just make a simple swap here when
		//the display changed
		this.scrollMaxX = Math.max(maxX,scroller.getCurrX());
	}
	
	//Internal method
	//Limit the scroll bounds
	public void setScrollMaxY(int maxY){
		if(maxY < 0){
			maxY = 0;
		}
		this.scrollMaxY = maxY;
		//widget size might changed
		//or text size is smaller
		//we should make measures
		if(getOffsetY() > maxY){
			scroller.startScroll(getOffsetX(),getOffsetY(),0,maxY-getOffsetY(),0);
			editor.invalidate();
		}
	}
	
	//Get current x
	public int getOffsetX(){
		return scroller.getCurrX();
	}
	
	//Get current y
	public int getOffsetY(){
		return scroller.getCurrY();
	}
	
	//Internal method
	//When the selection changed by other classes such as
	//SelectionController,we also call it to prevent to move
	//selection by onSingleTapUpConfirmed() but just make the
	//input method show
	public void setModification(boolean m){
		this.modification=m;
	}
	
	//Internal method
	//Keep the foucus {x,y}
	private void focusTo(float x,float y,float oldSize,float newSize){
		//Ignore small changes
		if((int)oldSize ==(int) newSize){
			return;
		}
		float rx = x,ry = y;
		x = getOffsetX() + x;
		y = getOffsetY() + y;
		x *= (newSize/oldSize);
		y *= (newSize/oldSize);
		x = x - rx;
		y = y - ry;
		if(x < 0){
			x = 0;
		}
		if(y < 0){
			y = 0;
		}
		scroller.startScroll(0,0,(int)x,(int)y,0);
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float newSize = detector.getScaleFactor() * editor.getStyles().getTextSize();
		//Too big
		if(newSize > 100){
			focusTo(detector.getFocusX(),detector.getFocusY(),editor.getStyles().getTextSize(),100);
			editor.getStyles().setTextSize(100);
			return false;
		}
		//Too small
		if(newSize < 20){
			focusTo(detector.getFocusX(),detector.getFocusY(),editor.getStyles().getTextSize(),20);
			editor.getStyles().setTextSize(20);
			return false;
		}
		//Keep focus
		focusTo(detector.getFocusX(),detector.getFocusY(),editor.getStyles().getTextSize(),newSize);
		editor.getStyles().setTextSize(newSize);
		scale = true;
		return scaleEnabled;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		scale = scaleEnabled;
		return scaleEnabled;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		scale = false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		if(scale){
			return false;
		}
		if(editor.isEditable() && editor.isEnabled()){
			boolean result = showSoftInputForEditor();
			if(!result){
				Log.w(LOG_TAG,"Unable to show soft input for editor:" + editor.toString());
			}
		}
		if(modification){
			modification = false;
			return true;
		}
		Selection.setSelection(editor.getEditableText(),editor.getCharOffsetByThumb(event.getX(),editor.getLineByThumbY(event.getY())));
		editor.notifySelChange();
		editor.invalidate();
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		if(scale){
			return false;
		}
		ClipboardManager cm = (ClipboardManager)editor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		editor.getEditableText().append(cm.getText());
		scroller.startScroll(scroller.getCurrX(),scroller.getCurrY(),0,scrollMaxY-getOffsetY(),0);
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		//do nothing
		return !scale;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		//return true
		//due to get following events
		if(!editor.isEnabled()){
			return false;
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		//do nothing
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		//*****deprecated*****
		//we use onSingleTapUpConfirmed()
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent event0, MotionEvent event1, float dx, float dy) {
		if(!scrollable||scale){
			return false;
		}
		if(scroller.getCurrX()+dx > scrollMaxX){
			dx = scrollMaxX - scroller.getCurrX();
		}else if(scroller.getCurrX()+dx < 0){
			dx = scroller.getCurrX();
		}
		if(scroller.getCurrY()+dy > scrollMaxY){
			dy = scrollMaxY - scroller.getCurrY();
		}else if(scroller.getCurrY()+dy < 0){
			dy = scroller.getCurrY();
		}
		scroller.forceFinished(true);
		scroller.startScroll(scroller.getCurrX(),scroller.getCurrY(),(int)dx,(int)dy,0);
		editor.invalidate();
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		if(!editor.isEnabled() || scale){
			return;
		}
		float x = event.getX();
		float y = event.getY();
		int line = editor.getLineByThumbY(y);
		if(line < 0){
			return;
		}
		int lineSt = editor.getLineStart(line);
		int lineEd = editor.getLineEnd(line);
		int thumb = editor.getCharOffsetByThumb(x,line);
		//Make a simple random to select
		int rand = (int)(Math.random()*(lineEd-lineEd)/3);
		if(rand == 0){
			rand = 2;
		}
		int left = thumb - rand;
		int right = thumb + rand;
		if(lineSt != lineEd){
			left = left < lineSt ? lineSt : left;
			right = right > lineEd ? lineEd : right;
		}
		editor.createSelectionControllerIfNeed();
		Selection.setSelection(editor.getEditableText(),left,right);
		modification = true;
		editor.invalidate();
	}

	@Override
	public boolean onFling(MotionEvent event0, MotionEvent event1, float vx, float vy) {
		if(!scrollable || dragMode || scale){
			return false;
		}
		scroller.forceFinished(true);
		scroller.fling(scroller.getCurrX(),scroller.getCurrY(),(int)-vx,(int)-vy,0,scrollMaxX,0,scrollMaxY,10,10);
		editor.invalidate();
		return true;
	}
	
	
}

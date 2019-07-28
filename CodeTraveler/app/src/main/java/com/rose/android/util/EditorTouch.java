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
	
	private CodeEditor editor;
	
	//This will be supported future
	private OverScroller scroller;
	
	private boolean scrollable;
	private boolean dragMode;
	private boolean scaleEnabled;
	
	private int scrollMaxX;
	private int scrollMaxY;
	
	public EditorTouch(CodeEditor e){
		this.editor = e;
		this.scroller = new OverScroller(editor.getContext());
		scroller.startScroll(0,0,0,0,0);
		this.scrollable = true;
		this.dragMode = false;
		this.scaleEnabled = true;
	}
	
	public void setDrag(boolean dragMode){
		this.dragMode = dragMode;
	}
	
	public boolean isDragMode(){
		return this.dragMode;
	}
	
	public void setScrollable(boolean scrollable){
		this.scrollable = scrollable;
	}
	
	public boolean canScroll(){
		return this.scrollable;
	}
	
	public void setScaleEnabled(boolean scale){
		this.scaleEnabled = scale;
	}
	
	public boolean canScaleByThumb(){
		return this.scaleEnabled;
	}
	
	public void resetForNewText(){
		scroller.forceFinished(true);
		scroller.startScroll(0,0,0,0,0);
		this.scrollMaxX = 0;
		this.scrollMaxY = 0;
	}
	
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
	
	public OverScroller getScroller(){
		return scroller;
	}
	
	public void setScrollMaxX(int maxX){
		if(maxX < 0){
			maxX = 0;
		}
		this.scrollMaxX = Math.max(maxX,scroller.getCurrX());
	}
	
	public void setScrollMaxY(int maxY){
		if(maxY < 0){
			maxY = 0;
		}
		this.scrollMaxY = maxY;
	}
	
	public int getOffsetX(){
		return scroller.getCurrX();
	}
	
	public int getOffsetY(){
		return scroller.getCurrY();
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float newSize = detector.getScaleFactor() * editor.getStyles().getTextSize();
		//Too big
		if(newSize > 100){
			editor.getStyles().setTextSize(100);
			return false;
		}
		//Too small
		if(newSize < 20){
			editor.getStyles().setTextSize(20);
			return false;
		}
		editor.getStyles().setTextSize(newSize);
		return scaleEnabled;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return scaleEnabled;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		//no nothing
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		if(editor.isEditable() && editor.isEnabled()){
			boolean result = showSoftInputForEditor();
			if(!result){
				Log.w(LOG_TAG,"Unable to show soft input for editor:" + editor.toString());
			}
		}
		Selection.setSelection(editor.getEditableText(),editor.getCharOffsetByThumb(event.getX(),editor.getLineByThumbY(event.getY())));
		editor.notifySelChange();
		editor.invalidate();
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		ClipboardManager cm = (ClipboardManager)editor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		editor.getEditableText().append(cm.getText());
		scroller.startScroll(scroller.getCurrX(),scroller.getCurrY(),0,scrollMaxY-getOffsetY(),0);
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		//do nothing
		return true;
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
		if(!scrollable){
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
		float x = event.getX();
		float y = event.getY();
		int line = editor.getLineByThumbY(y);
		if(line < 0){
			return;
		}
		int lineSt = editor.getLineStart(line);
		int lineEd = editor.getLineEnd(line);
		int thumb = editor.getCharOffsetByThumb(x,line);
		int rand = (int)(Math.random()*4);
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
		editor.invalidate();
	}

	@Override
	public boolean onFling(MotionEvent event0, MotionEvent event1, float vx, float vy) {
		if(!scrollable || dragMode){
			return false;
		}
		scroller.forceFinished(true);
		scroller.fling(scroller.getCurrX(),scroller.getCurrY(),(int)-vx,(int)-vy,0,scrollMaxX,0,scrollMaxY,10,10);
		editor.invalidate();
		return true;
	}
	
	
}

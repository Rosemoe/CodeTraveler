package com.rose.android.util;

import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import com.rose.android.widget.CodeEditor;
import android.view.GestureDetector.OnContextClickListener;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;

//TODO
public class EditorTouch implements OnGestureListener,OnContextClickListener,OnDoubleTapListener,OnScaleGestureListener
{
	private CodeEditor editor;
	private OverScroller scroller;
	
	public EditorTouch(CodeEditor e){
		this.editor = e;
		this.scroller = new OverScroller(editor.getContext());
		scroller.startScroll(0,0,0,0,0);
	}

	@Override
	public boolean onScale(ScaleGestureDetector p1) {
		
		return false;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector p1) {
		
		return false;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector p1) {
		
	}

	@Override
	public boolean onContextClick(MotionEvent p1) {
		
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent p1) {
		
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent p1) {
		
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent p1) {
		
		return false;
	}

	@Override
	public boolean onDown(MotionEvent event) {
		
		return false;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent event0, MotionEvent event1, float dx, float dy) {
		
		scroller.forceFinished(true);
		scroller.startScroll(scroller.getCurrX(),scroller.getCurrY(),(int)dx,(int)dy,0);
		editor.invalidate();
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		
	}

	@Override
	public boolean onFling(MotionEvent event0, MotionEvent event1, float vx, float vy) {
		
		return true;
	}
	
	
}

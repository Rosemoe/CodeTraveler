package com.rose.android.widget;

//Created By Rose on 2019/7/19

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.rose.android.util.EditorTouch;
import com.rose.android.util.a.EditorText;
import com.rose.android.util.a.TextWatcherR;
import android.widget.Toast;

public class CodeEditor extends View implements TextWatcherR {
	
	//TODO
	private EditorText mText;
	private Paint mPaint;
	private EditorStyle mStyle;
	
	private boolean mEditable;
	
	private EditorTouch mState;
	private GestureDetector mDetector_Basic;
	private ScaleGestureDetector mDetector_Scale;
	
	public CodeEditor(Context context){
		this(context,null);
	}
	
	public CodeEditor(Context context,AttributeSet attrs){
		this(context,attrs,0);
	}
	
	public CodeEditor(Context context,AttributeSet attrs,int styleResId){
		super(context,attrs,styleResId);
		initView();
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CodeEditor(Context context,AttributeSet attrs,int styleResId,int defStyle){
		super(context,attrs,styleResId,defStyle);
		initView();
	}
	
	private void initView(){
		mPaint = new TextPaint();
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.MONOSPACE);
		mPaint.setTextSize(50.0f);
		mStyle = new EditorStyle();
		mState = new EditorTouch(this);
		mDetector_Basic = new GestureDetector(getContext(),mState);
		mDetector_Scale = new ScaleGestureDetector(getContext(),mState);
		mDetector_Basic.setContextClickListener(mState);
		mDetector_Basic.setOnDoubleTapListener(mState);
		this.setText("Hello World!\nThis is test activity");
		this.setEditable(true);
		super.setFocusable(true);
		super.setFocusableInTouchMode(true);
	}
	
	//---------------------------------------
	
	public void setText(CharSequence text){
		if(mText != null){
			mText.removeWatcher(this);
		}
		mText = new EditorText(text);
		mText.addWatcher(this);
		mState.resetForNewText();
		requestLayout();
		invalidate();
	}
	
	public Editable getEditableText(){
		return mText;
	}
	
	public String getText(){
		return mText.toString();
	}
	
	//---------------------------------------
	
	public EditorStyle getStyles(){
		return mStyle;
	}
	
	//---------------------------------------
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		if(!isEnabled()){
			return false;
		}
		int thumb = event.getPointerCount();
		if(thumb == 1){
			return mDetector_Basic.onTouchEvent(event)||mDetector_Scale.onTouchEvent(event);
		}else if(thumb == 2){
			return mDetector_Scale.onTouchEvent(event);
		}else{
			return false;
		}
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if(isEnabled()){
			return false;
		}
		return mDetector_Basic.onGenericMotionEvent(event);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		//we do not calculate our width beacause it might use plenty of time
		if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST){
			//fiil all the available area by default
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY,MeasureSpec.getSize(widthMeasureSpec));
		}
		
		if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST){
			int height = (int)mStyle.getLineHeight() * getLineCount();
			int aHeight = MeasureSpec.getSize(heightMeasureSpec);
			if(height > aHeight){
				height = aHeight;
			}
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY,height);
		}
		
		mState.setScrollMaxX(MeasureSpec.getSize(widthMeasureSpec)*3);
		mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - MeasureSpec.getSize(heightMeasureSpec)/2);
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	//---------------------------------------
	
	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		//Toast.makeText(getContext(),Integer.toString(getLineCount()),0).show();
		drawLineNumbers(canvas);
		drawDividerLine(canvas);
		float cost = mPaint.measureText(Integer.toString(getLineCount())) + 10f;
		for(int i = getFirstVisableLine();i <= getLastVisableLine();i++){
			//canvas.drawText(Integer.toString(i+1),-mState.getOffsetX(),getLineBaseline(i)-mState.getOffsetY(),mPaint);
			canvas.drawText(mText,getLineStart(i),getLineEnd(i),10+cost-mState.getOffsetX(),getLineBaseline(i)-mState.getOffsetY(),mPaint);
			//float maxX = mPaint.measureText(mText,getLineStart(i),getLineEnd(i));
			//mState.setScrollMaxX((int)maxX);
		}
	}
	
	private void drawLineNumbers(Canvas canvas){
		int i = getFirstVisableLine();
		int m = getLastVisableLine();
		for(;i <= m;i++){
			canvas.drawText(Integer.toString(i+1), - mState.getOffsetX(), getLineBaseline(i) - mState.getOffsetY(),mPaint);
		}
	}
	
	private void drawDividerLine(Canvas canvas){
		float left = mPaint.measureText(Integer.toString(getLineCount()));
		canvas.drawLine(left-mState.getOffsetX()+5,0,left-mState.getOffsetX()+10,getHeight(),mPaint);
	}

	@Override
	public void computeScroll() {
		if(mState.getScroller().computeScrollOffset()){
			invalidate();
		}
		super.computeScroll();
	}
	
	//---------------------------------------
	
	public int getFirstVisableLine(){
		return (mState.getOffsetY()/(int)mStyle.getLineHeight())-1;
	}
	
	public int getLastVisableLine(){
		int l = (mState.getOffsetY() + getHeight())/(int)getLineBaseline(0) + 2;
		return (l<getLineCount()?l:getLineCount() -1);
	}
	
	public float getLineBaseline(int line){
		//TODO
		return mStyle.getLineHeight() * (line)+ mPaint.getTextSize() * 3 / 4;
	}
	
	public int getLineCount(){
		return mText.getLineCount();
	}

	public int getLineStart(int line){
		return mText.getLineStart(line);
	}

	public int getLineEnd(int line){
		return mText.getLineEnd(line);
	}

	public int getLineByIndex(int charIndex){
		return mText.getLineByIndex(charIndex);
	}

	//---------------------------------------
	
	@Override
	public void onReplace(Editable doc) {
		//do nothing
	}

	@Override
	public void onInsert(Editable doc, int index, CharSequence textToInsert) {
		mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - getHeight()/2);
		
		invalidate();
		//TODO
	}

	@Override
	public void onDelete(Editable doc, int index, CharSequence textDeleted) {
		mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - getHeight()/2);
		invalidate();
		//TODO
	}
	
	//---------------------------------------

	public boolean isEditable(){
		return mEditable;
	}
	
	public void setEditable(boolean editable){
		mEditable = editable;
	}

	@Override
	public boolean onCheckIsTextEditor() {
		return isEditable();
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT;
		outAttrs.initialSelStart = Selection.getSelectionStart(mText);
		outAttrs.initialSelEnd = Selection.getSelectionEnd(mText);
		mText.resetBatchEdit();
		return (isEditable()) ? new EditorInputConnection() : null;
	}
	
	//---------------------------------------
	
	public class EditorInputConnection extends BaseInputConnection{
		
		public EditorInputConnection(){
			super(CodeEditor.this,true);
		}

		@Override
		public boolean commitText(CharSequence text, int newCursorPosition) {
			return super.commitText(text,newCursorPosition);
		}

		@Override
		public boolean deleteSurroundingText(int beforeLength, int afterLength) {
			return super.deleteSurroundingText(beforeLength, afterLength);
		}

		@Override
		public Editable getEditable() {
			return isEditable() ? getEditableText() : null;
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			if(event.getAction() == event.ACTION_DOWN){
				switch(event.getKeyCode()){
					case KeyEvent.KEYCODE_DEL:
						deleteSurroundingText(1,0);
						return true;
					case KeyEvent.KEYCODE_ENTER:
						commitText("\n",1);
						return true;
				}
			}
			return super.sendKeyEvent(event);
		}

		@Override
		public boolean beginBatchEdit() {
			mText.beginBatchEdit();
			return super.beginBatchEdit();
		}

		@Override
		public boolean endBatchEdit() {
			mText.endBatchEdit();
			return super.endBatchEdit();
		}
		
	}
	
	//---------------------------------------
	
	public class EditorStyle{
		
		private int defaultTextColor = Color.BLACK;
		
		private boolean distsncePixelOrDouble;
		
		private float distance;
		
		private EditorStyle(){
			distsncePixelOrDouble = true;
			distance = 5;
		}
		
		public void setTypeface(Typeface typeface){
			if(typeface == null){
				typeface = Typeface.MONOSPACE;
			}
			mPaint.setTypeface(typeface);
			invalidate();
		}
		
		public Typeface getTypeface(){
			return mPaint.getTypeface();
		}
		
		public void setTextSize(float size){
			mPaint.setTextSize(size);
			mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - getHeight()/2);
			invalidate();
		}
		
		public float getTextSize(){
			return mPaint.getTextSize();
		}
		
		public void setTextScaleX(float scaleX){
			mPaint.setTextScaleX(scaleX);
			invalidate();
		}
		
		public float getTextScaleX(){
			return mPaint.getTextScaleX();
		}
		
		public float getTextSkewX(){
			return mPaint.getTextSkewX();
		}
		
		public void setTextSkewX(float skewX){
			mPaint.setTextSkewX(skewX);
			invalidate();
		}
		
		public float getLineHeight(){
			return (getLineRealHeight() + getLineDistancePixel());
		}
		
		public float getLineRealHeight(){
			return (mPaint.descent() - mPaint.ascent());
		}
		
		public float getLineDistancePixel(){
			return (distsncePixelOrDouble ? distance : getLineRealHeight() * distance);
		}
		
		public void setLineDistancePixel(float pixel){
			if(pixel < 0){
				throw new IllegalArgumentException("under zero is not allowed");
			}
			distsncePixelOrDouble = true;
			distance = pixel;
			invalidate();
		}
		
		public void setLineDistanceDouble(float d){
			if(d >= 0 && d <= 1){
				distsncePixelOrDouble = false;
				distance = d;
			}else{
				throw new IllegalArgumentException("only 0~1 are accepted");
			}
			invalidate();
		}
		
		public void setDefaultTextColor(int color){
			this.defaultTextColor = color;
			invalidate();
		}
		
		public int getDefaultTextColor(){
			return this.defaultTextColor;
		}
		
	}
	
}

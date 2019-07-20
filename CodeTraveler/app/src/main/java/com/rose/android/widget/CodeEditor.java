package com.rose.android.widget;

//Created By Rose on 2019/7/19

import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.annotation.TargetApi;
import android.os.Build;
import android.graphics.Paint;
import android.text.TextPaint;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.inputmethod.BaseInputConnection;
import android.text.Editable;
import com.rose.android.util.EditorText;
import android.view.KeyEvent;
import android.graphics.Canvas;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.EditorInfo;
import android.text.Selection;
import android.widget.OverScroller;
import android.view.MotionEvent;
import com.rose.android.util.EditorTouch;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;

//Created By Rose on 2019/7/20

public class CodeEditor extends View implements EditorText.DocumentChangeListener {
	
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
		//TODO
		if(mText != null){
			mText.removeDocumentChangeListener(this);
		}
		mText = new EditorText(text);
		mText.addDocumentChangeListener(this);
		
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
		return mDetector_Basic.onTouchEvent(event)||mDetector_Scale.onTouchEvent(event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		return mDetector_Basic.onGenericMotionEvent(event);
	}
	
	//---------------------------------------
	
	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		for(int i = getFirstVisableLine();i <= getLastVisableLine();i++){
			canvas.drawText(mText,(i!=0)?getLineStart(i)+1:0,getLineEnd(i),10,getLineBaseline(i),mPaint);
			//canvas.drawLine(0,getLineBaseline(i),getWidth(),getLineBaseline(i)+2,mPaint);
		}
	}
	
	//---------------------------------------
	
	public int getFirstVisableLine(){
		//TODO
		return 0;
	}
	
	public int getLastVisableLine(){
		//TODO
		return getLineCount() - 1;
	}
	
	public float getLineBaseline(int line){
		//TODO
		return mPaint.getTextSize() * (line)+ mPaint.getTextSize() * 3 / 4;
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
	public void onReplace(EditorText doc) {
		//do nothing
	}

	@Override
	public void onInsert(EditorText doc, int index, CharSequence textToInsert) {
		invalidate();
		//TODO
	}

	@Override
	public void onDelete(EditorText doc, int index, CharSequence textDeleted) {
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
		return (isEditable()) ? new EditorInputConnection() : null;
	}
	
	//---------------------------------------
	
	public class EditorInputConnection extends BaseInputConnection{
		
		public EditorInputConnection(){
			super(CodeEditor.this,true);
		}

		@Override
		public Editable getEditable() {
			return isEditable()?getEditableText():null;
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			//TODO
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

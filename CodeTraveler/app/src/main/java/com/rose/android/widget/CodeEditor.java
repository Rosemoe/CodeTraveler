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

public class CodeEditor extends View {
	
	private Paint mPaint;
	private EditorStyle mStyle;
	
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
		mStyle = new EditorStyle();
	}
	
	public EditorStyle getStyle(){
		return mStyle;
	}
	
	public Editable getEditableText(){
		return null;
	}
	
	public int getLineCount(){
		return 0;
	}
	
	public int getLineStart(int line){
		return 0;
	}
	
	public int getLineEnd(int line){
		return 0;
	}
	
	public int getLineByIndex(int charIndex){
		return 0;
	}
	
	public class EditorInputConnection extends BaseInputConnection{
		
		public EditorInputConnection(){
			super(CodeEditor.this,true);
		}

		@Override
		public Editable getEditable() {
			return CodeEditor.this.getEditableText();
		}

		@Override
		public boolean beginBatchEdit() {
			//TODO send to Document
			return super.beginBatchEdit();
		}

		@Override
		public boolean endBatchEdit() {
			//TODO send to Document
			return super.endBatchEdit();
		}
		
	}
	
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
		}
		
		public Typeface getTypeface(){
			return mPaint.getTypeface();
		}
		
		public void setTextSize(float size){
			mPaint.setTextSize(size);
		}
		
		public float getTextSize(){
			return mPaint.getTextSize();
		}
		
		public void setTextScaleX(float scaleX){
			mPaint.setTextScaleX(scaleX);
		}
		
		public float getTextScaleX(){
			return mPaint.getTextScaleX();
		}
		
		public float getTextSkewX(){
			return mPaint.getTextSkewX();
		}
		
		public void setTextSkewX(float skewX){
			mPaint.setTextSkewX(skewX);
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
		}
		
		public void setLineDistanceDouble(float d){
			if(d >= 0 && d <= 1){
				distsncePixelOrDouble = false;
				distance = d;
			}else{
				throw new IllegalArgumentException("only 0~1 are accepted");
			}
		}
		
		public void setDefaultTextColor(int color){
			this.defaultTextColor = color;
		}
		
		public int getDefaultTextColor(){
			return this.defaultTextColor;
		}
		
	}
	
}

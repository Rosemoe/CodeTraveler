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
	
	//the using text object
	private EditorText mText;
	
	//our canvas paint
	private Paint mPaint;
	
	//all of our styles are provided by it
	private EditorStyle mStyle;
	
	//is the editor editable
	private boolean mEditable;
	
	//our states which will be changed by user touch actions saved in it
	//as well as manage them
	private EditorTouch mState;
	
	//detect the actions of scroll or click
	private GestureDetector mDetector_Basic;
	
	//detect the actions of scale text size
	private ScaleGestureDetector mDetector_Scale;
	
	//height wrap mode
	private boolean height_wrap;
	
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
	
	//Constructor for Lollipop +
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CodeEditor(Context context,AttributeSet attrs,int styleResId,int defStyle){
		super(context,attrs,styleResId,defStyle);
		initView();
	}
	
	//init the view
	private void initView(){
		mPaint = new TextPaint();
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.MONOSPACE);
		mPaint.setTextSize(50.0f);
		height_wrap = false;
		mStyle = new EditorStyle();
		mState = new EditorTouch(this);
		mDetector_Basic = new GestureDetector(getContext(),mState);
		mDetector_Scale = new ScaleGestureDetector(getContext(),mState);
		mDetector_Basic.setContextClickListener(mState);
		mDetector_Basic.setOnDoubleTapListener(mState);
		this.setText("");
		this.setEditable(true);
		super.setFocusable(true);
		super.setFocusableInTouchMode(true);
	}
	
	//---------------------------------------
	
	//set a new text for the view
	public void setText(CharSequence text){
		if(mText != null){
			//disconnect with previous text
			//or we may will receive some meaningless event
			//and wrongly update the display
			mText.removeWatcher(this);
		}
		//create text
		mText = new EditorText(text);
		//add listener to the text created
		//so that we can updated our display on time
		mText.addWatcher(this);
		//reset states
		mState.resetForNewText();
		
		//request layout due to attribute "wrap_content"
		requestLayout();
		//refresh display
		invalidate();
	}
	
	//get the text object
	//Actually only return the type of EditorText
	public Editable getEditableText(){
		return mText;
	}
	
	//get normal text
	//it will spend a long time if there is a big text
	public String getText(){
		return mText.toString();
	}
	
	//---------------------------------------
	
	//get style manager to control the look
	public EditorStyle getStyles(){
		return mStyle;
	}
	
	//---------------------------------------
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		//disabled.cancel the event;
		if(!isEnabled()){
			return false;
		}
		
		int thumb = event.getPointerCount();//thumb count
		if(thumb == 1){
			//might a scale start
			//so we must send the event to the two detectors
			
			//this ensure that we send event to the two detectors
			boolean b = mDetector_Basic.onTouchEvent(event);
			boolean c = mDetector_Scale.onTouchEvent(event);
			
			return b||c;
		}else if(thumb == 2){
			//too thumbs now
			//due to scale corrently,we only send to one dector;
			return mDetector_Scale.onTouchEvent(event);
		}else{
			//thumb is more than 2
			//unknown action
			return false;
		}
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		//disabled,cancel event
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
		
		//height is wrap_content mode
		if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST){
			//calculate our content height needed
			int height = (int)mStyle.getLineHeight() * getLineCount();
			//available height
			int aHeight = MeasureSpec.getSize(heightMeasureSpec);
			//compare
			if(height > aHeight){
				height = aHeight;
			}
			//set the min height
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY,height);
			//flag so that layout will update when the text changed
			height_wrap = true;
		}else{
			height_wrap = false;
		}
		
		//scroll max x(TODO)
		mState.setScrollMaxX(MeasureSpec.getSize(widthMeasureSpec)*3);
		//scroll max y
		mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - MeasureSpec.getSize(heightMeasureSpec)/2);
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	//---------------------------------------
	
	private float offsetX;
	
	@Override
	protected void onDraw(Canvas canvas){
		//call super to draw background.
		super.onDraw(canvas);
		offsetX = -mState.getOffsetX();
		
		if(mStyle.showLineNumber){
			drawLineNumbers(canvas);
			drawDividerLine(canvas);
		}
		
		drawText_debug(canvas);
	}
	
	private void drawText_debug(Canvas canvas){
		mPaint.setColor(mStyle.defaultTextColor);
		int i = getFirstVisableLine();
		int m = getLastVisableLine();
		for(;i<=m;i++){
			drawLine_debug(mText,getLineStart(i),getLineEnd(i),offsetX,getLineBaseline(i)-mState.getOffsetY(),canvas);
		}
	}
	
	private void drawLine_debug(CharSequence s,int st,int en,float x,float y,Canvas c){
		for(int i = st;i < en;i++){
			if(s.charAt(i)=='\t'){
				x += mPaint.measureText("    ");
			}else{
				c.drawText(s,i,i+1,x,y,mPaint);
				x += mPaint.measureText(s,i,i+1);
			}
		}
	}
	
	private void drawLineNumbers(Canvas canvas){
		mPaint.setColor(mStyle.lineNumberColor);
		int i = getFirstVisableLine();
		int m = getLastVisableLine();
		offsetX += 5;
		for(;i <= m;i++){
			canvas.drawText(Integer.toString(i+1), offsetX, getLineBaseline(i) - mState.getOffsetY(),mPaint);
		}
		//tip:5 is the right margin of line number
		//and the left margib of line number
		offsetX += mPaint.measureText(Integer.toString(getLineCount() + 1)) + 5;
	}
	
	private void drawDividerLine(Canvas canvas){
		mPaint.setColor(mStyle.dividerLineColor);
		//5 is the width of divider
		canvas.drawRect(offsetX,0,offsetX+5,getHeight(),mPaint);
		//extra 5 is the right margin
		offsetX += 10;
	}

	@Override
	public void computeScroll() {
		//Override due to scroll effects
		if(mState.getScroller().computeScrollOffset()){
			invalidate();
		}
		super.computeScroll();
	}
	
	//---------------------------------------
	
	private void calculateScrollMaxY(){
		mState.setScrollMaxY(getLineCount() * (int) (mStyle.getLineHeight()) - getHeight()/2);
	}
	
	public int getFirstVisableLine(){
		int i = mState.getOffsetY()/(int)(mStyle.getLineHeight());
		return (i>=0)?i:0;
	}
	
	public int getLastVisableLine(){
		int l = (int)Math.ceil((mState.getOffsetY()+getHeight())/mStyle.getLineHeight());
		return (l<getLineCount()?l:getLineCount() -1);
	}
	
	public float getLineBaseline(int line){
		return mStyle.getLineHeight() * ( line + 1) - mPaint.getFontMetrics().descent;
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
	
	//this flag is used to tell us whether the delete action is a part of replace
	//so we will not redraw twice when the text changing
	private boolean flagReplace = false;
	
	@Override
	public void onReplace(Editable doc) {
		flagReplace = true;
	}

	@Override
	public void onInsert(Editable doc, int index, CharSequence textToInsert) {
		if(height_wrap){
			requestLayout();
		}
		calculateScrollMaxY();
		invalidate();
	}

	@Override
	public void onDelete(Editable doc, int index, CharSequence textDeleted) {
		if(flagReplace){
			flagReplace = false;
			return;
		}
		calculateScrollMaxY();
		if(height_wrap){
			requestLayout();
		}
		invalidate();
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
		//when we create a connection
		//we reset the batch edit state
		mText.resetBatchEdit();
		return (isEditable()) ? new EditorInputConnection() : null;
	}
	
	//---------------------------------------
	
	public class EditorInputConnection extends BaseInputConnection{
		
		public EditorInputConnection(){
			super(CodeEditor.this,true);
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
		private int lineNumberColor = 0xff3f51b5;
		private int dividerLineColor = 0xff3f51b5;
		private boolean showLineNumber = true;
		private boolean distsncePixelOrDouble;
		private float distance;
		
		private EditorStyle(){
			distsncePixelOrDouble = true;
			distance = 0;
		}
		
		public void setTypeface(Typeface typeface){
			if(typeface == null){
				typeface = Typeface.MONOSPACE;
			}
			mPaint.setTypeface(typeface);
			calculateScrollMaxY();
			invalidate();
		}
		
		public Typeface getTypeface(){
			return mPaint.getTypeface();
		}
		
		public void setTextSize(float size){
			mPaint.setTextSize(size);
			calculateScrollMaxY();
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
			Paint.FontMetricsInt ints = mPaint.getFontMetricsInt();
			return  (ints.descent - ints.ascent);
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
			calculateScrollMaxY();
			invalidate();
		}
		
		public void setLineDistanceDouble(float d){
			if(d >= 0 && d <= 1){
				distsncePixelOrDouble = false;
				distance = d;
			}else{
				throw new IllegalArgumentException("only 0~1 are accepted");
			}
			calculateScrollMaxY();
			invalidate();
		}
		
		public void setShowLineNumber(boolean s){
			showLineNumber = s;
			invalidate();
		}
		
		public void setDefaultTextColor(int color){
			this.defaultTextColor = color;
			invalidate();
		}
		
		public int getDefaultTextColor(){
			return this.defaultTextColor;
		}
		
		public void setLineNumberColor(int c){
			lineNumberColor=c;
			invalidate();
		}
		
		public void setDividerColor(int c){
			dividerLineColor=c;
			invalidate();
		}
	}
	
}

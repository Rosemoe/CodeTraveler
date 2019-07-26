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
import com.rose.android.Debug;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import com.rose.android.util.a.EditorInputConnection;
import android.widget.TextView;
import android.view.Gravity;
import android.util.Log;

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
	
	//input method service
	private InputMethodManager mIMM;
	private EditorInputConnection conn;
	
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
	
	//init the view
	private void initView(){
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.MONOSPACE);
		mPaint.setTextSize(50.0f);
		height_wrap = false;
		mStyle = new EditorStyle();
		mStyle.setTextSize(50.0f);
		mStyle.setLineNumberGravity(Gravity.RIGHT);
		mState = new EditorTouch(this);
		mDetector_Basic = new GestureDetector(getContext(),mState);
		mDetector_Scale = new ScaleGestureDetector(getContext(),mState);
		mDetector_Basic.setOnDoubleTapListener(mState);
		mIMM = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
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
		Selection.setSelection(mText,0);
		//request layout due to attribute "wrap_content"
		requestLayout();
		//refresh display
		invalidate();
		//tell the input method
		mIMM.restartInput(this);
	}
	
	//get the text object
	//Actually only return the type of EditorText
	public EditorText getEditableText(){
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
	
	//do not.call it yourself!
	public void notifySelChange(){
		if(conn == null||conn.isBatchEdit()||!hasFocus()){
			return;
		}
		int st = Selection.getSelectionStart(mText);
		int ed = Selection.getSelectionEnd(mText);
		mIMM.updateSelection(this,st,ed,conn.getComposingSpanStart(mText),conn.getComposingSpanEnd(mText));
		//Debug.debug("Sel upd");
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
	private int CurrLine;
	private int selSt;
	private float tabSize;
	
	public float getLastOffset(){
		return offsetX;
	}
	
	public float measureText(int st,int ed){
		float j = 0;
		char b[] =new char[1];
		while(st < ed){
			if(mText.charAt(st)=='\t'){
				j += 4 * mPaint.measureText(" ");
			}else{
				b[0]=mText.charAt(st);
				j += mPaint.measureText(b,0,1);
			}
			st++;
		}
		return j;
	}
	
	@Override
	protected void onDraw(Canvas canvas){
		//call super to draw background.
		super.onDraw(canvas);
		offsetX = -mState.getOffsetX();
		
		drawCurrLineBackground(canvas);
		
		if(mStyle.showLineNumber){
			drawLineNumbers(canvas);
			drawDividerLine(canvas);
		}
		
		drawText_debug(canvas);
	}
	
	private void drawText_debug(Canvas canvas){
		mPaint.setColor(mStyle.defaultTextColor);
		int i = getFirstVisibleLine();
		int m = getLastVisibleLine();
		selSt = Selection.getSelectionStart(mText);
		tabSize = 4 * mPaint.measureText(" ");
		for(;i<=m;i++){
			drawLine_debug(mText,i,offsetX,getLineBaseLineOnScreen(i),canvas);
		}
	}
	
	private void drawLine_debug(CharSequence s,int li,float x,float y,Canvas c){
		int st = getLineStart(li);
		int en = getLineEnd(li);
		int start = selSt;
		if(true){
			String str = mText.subSequence(st,en).toString().replace("\t","    ");
			c.drawText(str,x,y,mPaint);
			if(st<=start && en >= start && li == CurrLine){
				float xf = measureText(st,start) + x;
				drawCursorIfHereIs(start,start-1,xf,li,c);
			}
			return;
		}
		
		//Deprecated
		char buffer[] = new char[1];
		drawCursorIfHereIs(start,st - 1,x,li,c);
		for(int i = st;i < en;i++){
			if(s.charAt(i)=='\t'){
				x = x + tabSize;
			}else{
				c.drawText(s,i,i+1,x,y,mPaint);
				buffer[0] = s.charAt(i);
				x = x + mPaint.measureText(buffer,0,1);
			}
			drawCursorIfHereIs(start,i,x,li,c);
			if(x > getWidth()){
				break;
			}
		}
	}
	
	private void drawCursorIfHereIs(int cursorPos,int index,float offsetX,int line,Canvas c){
		if(cursorPos == index + 1){
			int backup = mPaint.getColor();
			mPaint.setColor(Color.BLACK);
			mPaint.setStrokeWidth(2);
			c.drawLine(offsetX,getLineTopOnScreen(line),offsetX,getLineBottomOnScreen(line),mPaint);
			mPaint.setColor(backup);
		}
	}
	
	private void drawCurrLineBackground(Canvas canvas){
		int charOffset = Selection.getSelectionStart(mText);
		try{
			if(mText.charAt(charOffset)=='\n' && charOffset != 0){
				charOffset--;
			}
		}catch(Exception e){
			
		}
		int line = getLineByIndex(charOffset);
		CurrLine = line;
		mPaint.setColor(mStyle.lineColor);
		float top = getLineTopOnScreen(line);
		float bot = top + mStyle.getLineHeight();
		canvas.drawRect(0,top,getWidth(),bot,mPaint);
	}
	
	private void drawLineNumbers(Canvas canvas){
		int i = getFirstVisibleLine();
		int m = getLastVisibleLine();
		float x = 0;
		float width = mPaint.measureText(Integer.toString(getLineCount() + 1));
		drawLineNumberBackground(canvas,offsetX,offsetX+width+10);
		mPaint.setColor(mStyle.lineNumberColor);
		int gv = mStyle.lnGravity;
		switch(gv){
			case Gravity.LEFT:
				x = offsetX;
				mPaint.setTextAlign(Paint.Align.LEFT);
				break;
			case Gravity.RIGHT:
				x = offsetX + width;
				mPaint.setTextAlign(Paint.Align.RIGHT);
				break;
			case Gravity.CENTER:
				mPaint.setTextAlign(Paint.Align.CENTER);
				x = offsetX + width/2;
				break;
		}
		x += 5;
		for(;i <= m;i++){
			canvas.drawText(Integer.toString(i+1), x, getLineBaseLineOnScreen(i),mPaint);
		}
		//tip:5 is the right margin of line number
		//and the left margin of line number
		offsetX += width + 10;
		mPaint.setTextAlign(Paint.Align.LEFT);
	}
	
	private void drawDividerLine(Canvas canvas){
		mPaint.setColor(mStyle.dividerLineColor);
		//5 is the width of divider
		canvas.drawRect(offsetX,0,offsetX+5,getHeight(),mPaint);
		//extra 5 is the right margin
		offsetX += 10;
	}
	
	private void drawLineNumberBackground(Canvas canvas,float from,float to){
		mPaint.setColor(mStyle.LNBG);
		canvas.drawRect(from,0,to,getHeight(),mPaint);
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
		mState.setScrollMaxY(getLineCount() * ((int)mStyle.getLineHeight()) - getHeight()/2);
	}
	
	public int getFirstVisibleLine(){
		int i = mState.getOffsetY()/((int)mStyle.getLineHeight());
		return (i>=0)?i:0;
	}
	
	public int getLastVisibleLine(){
		int l = (int)Math.ceil((mState.getOffsetY()+getHeight())/mStyle.getLineHeight());
		return (l<getLineCount()?l:getLineCount() -1);
	}
	
	public long getLineBaseLineOnScreen(int line){
		return (long)getLineBaseline(line - getFirstVisibleLine())-getOffsetOnScreen();
	}
	
	public long getLineTopOnScreen(int line){
		return (long)mStyle.getLineTop(line - getFirstVisibleLine()) - getOffsetOnScreen();
	}
	
	public long getLineBottomOnScreen(int line){
		return (long)mStyle.getLineBottom(line - getFirstVisibleLine()) - getOffsetOnScreen();
	}
	
	long getOffsetOnScreen(){
		long offY = mState.getOffsetY();
		long fl = getFirstVisibleLine();
		long newOff = offY - (long)mStyle.getLineHeight()*fl;
		return newOff;
	}
	
	public float getLineBaseline(int line){
		float top = mStyle.top;
		float bottom = mStyle.bottom;
		return mStyle.getLineHeight() * (line + 0.5f) + (top-bottom)/2;
	}
	
	public int getLineCount(){
		return mText.getLineCount();
	}

	public int getLineStart(int line){
		//due to not get the wrong start
		//beacause LineManager ususlly return
		//the '\n' as the line start
		//we would like to prevent it
		int i = mText.getLineStart(line);
		if(line != 0){
			i++;
		}
		return i;
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
		notifySelChange();
		invalidate();
	}

	@Override
	public void onDelete(Editable doc, int index, CharSequence textDeleted) {
		if(flagReplace){
			flagReplace = false;
			return;
		}
		notifySelChange();
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
		return (isEditable()) ? (conn = new EditorInputConnection(this)) : null;
	}
	
	//---------------------------------------
	
	public class EditorStyle{
		
		private int defaultTextColor = Color.BLACK;
		private int lineColor = 0x66ec407a;
		private int lineNumberColor = 0xff3f51b5;
		private int dividerLineColor = 0xff3f51b5;
		private int LNBG = 0xeeeeeeee;
		private boolean showLineNumber = true;
		private boolean distsncePixelOrDouble;
		private float distance;
		private float top,bottom,ascent,descent;
		private int lnGravity;
		
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
			Paint.FontMetrics fm = mPaint.getFontMetrics();
			top = Math.abs(fm.top);
			bottom = Math.abs(fm.bottom);
			ascent = Math.abs(fm.ascent);
			descent = Math.abs(fm.descent);
			if(mText != null)
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
		
		public float getLineTop(int line){
			return getLineHeight() * line;
		}
		
		public float getLineBottom(int line){
			return getLineTop(line) + getLineHeight();
		}
		
		public float getLineRealHeight(){
			return (float)Math.ceil(descent) + (float)Math.ceil(ascent);
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
		
		public int getLineNumberColor(){
			return lineNumberColor;
		}
		
		public void setDividerColor(int c){
			dividerLineColor=c;
			invalidate();
		}
		
		public int getDividerColor(){
			return dividerLineColor;
		}
		
		public void setLineNumberBackground(int c){
			LNBG =c;
			invalidate();
		}
		
		public int getLineNumberBackground(int c){
			return LNBG;
		}
		
		public void setCurrentLineColor(int c){
			lineColor = c;
		}
		
		public int getCurrentLineColor(){
			return lineColor;
		}
		
		public void setLineNumberGravity(int gravity){
			switch(gravity){
				case Gravity.CENTER:
				case Gravity.LEFT:
				case Gravity.RIGHT:
					if(gravity == lnGravity){
						//avoid useless invalidate() calls
						return;
					}
					lnGravity = gravity;
					invalidate();
					break;
				case Gravity.CENTER_HORIZONTAL:
					setLineNumberGravity(Gravity.CENTER);
					Log.w("CodeEditor","CENTER_HORIZONTAL is not a valid flag for CodeEditor,please use CENTER");
					break;
				default:
					throw new IllegalArgumentException("Gravity not supported");
			}
		}
		
		public int getLineNumberFravity(){
			return lnGravity;
		}
	}
	
}

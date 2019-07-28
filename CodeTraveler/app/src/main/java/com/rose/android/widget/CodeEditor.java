package com.rose.android.widget;

//Created By Rose on 2019/7/19

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import com.rose.android.util.EditorTouch;
import com.rose.android.util.a.EditorInputConnection;
import com.rose.android.util.a.EditorText;
import com.rose.android.util.a.SelectionController;
import com.rose.android.util.a.TextWatcherR;
import com.rose.android.Debug;

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
	//handle the selection movement
	private SelectionController selController;
	
	//input method service
	private InputMethodManager mIMM;
	private EditorInputConnection conn;
	
	//height wrap mode
	private boolean height_wrap;
	
	/*
	 * Simple Constructor for Java code
	 */
	public CodeEditor(Context context){
		this(context,null);
	}
	
	/*
	 * Constructor for Android Xml Parser
	 * AttributeSet will be supported future
	 */
	public CodeEditor(Context context,AttributeSet attrs){
		this(context,attrs,0);
	}
	
	/*
	 * Constructor for Android Xml Parser
	 * AttributeSet will be supported future
	 * style will be supported future
	 */
	public CodeEditor(Context context,AttributeSet attrs,int styleResId){
		super(context,attrs,styleResId);
		initView();
	}
	
	/*
	 * initView():void
	 * the basic init actions will be done here
	 * it will be called only when the view's construcutor is called
	 */
	private void initView(){
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.MONOSPACE);
		mPaint.setTextSize(50.0f);
		height_wrap = false;
		//Style manager
		mStyle = new EditorStyle();
		mStyle.setTextSize(50.0f);
		mStyle.setLineNumberGravity(Gravity.RIGHT);
		mState = new EditorTouch(this);
		//To detect gestures
		mDetector_Basic = new GestureDetector(getContext(),mState);
		mDetector_Scale = new ScaleGestureDetector(getContext(),mState);
		mDetector_Basic.setOnDoubleTapListener(mState);
		//To concat with input method
		mIMM = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		this.setText("");
		this.setEditable(true);
		//To show input method
		super.setFocusable(true);
		super.setFocusableInTouchMode(true);
	}
	
	//---------------------------------------
	
	/*
	 * setText(CharSequence):void
	 * set a new text for the view to display
	 * 
	 * @param text the text you want to set.
	 * The object will never change as 
	 * we will make a copy for its chars to the EditorText
	 * 
	 */
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
	
	/*
	 * getEditableText():EditorText
	 * 
	 * @return the EditorText we are using
	 * by calling this method,you can get the
	 * using text with toString() and the speed will increase
	 * you can also make changes to the text displaying with it
	 */
	public EditorText getEditableText(){
		return mText;
	}
	
	/*
	 * getText():String
	 * It will spend a long time if there is a big text
	 *
	 * @return Just as its method name...
	 */
	public String getText(){
		return mText.toString();
	}
	
	//---------------------------------------
	
	/*
	 * getStyles():EditorStyle
	 *
	 * All of our appearance settings are saved in this object
	 * you can make changes to it
	 * so that you can easily make its style better
	 */
	public EditorStyle getStyles(){
		return mStyle;
	}
	
	/*
	 * notifySelChange()
	 * 
	 * This method is for internal calls
	 * It sends the current slection positions to
	 * the InputMethodManager due to notify the input method
	 * that the selection positions have changed
	 * The input method will make wrong actions with
	 * InputConnection#setSelection(int,int) to set the selection
	 * a unexpected position
	 *
	 * About more information plaease see
	 * InputMethodManager#updateSelection()
	 */
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
	
	private boolean eventToSelCon = false;
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		//disabled.cancel the event;
		if(!isEnabled()){
			return false;
		}
		
		if(selController != null && getStart() != getEnd()){
			switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					eventToSelCon = selController.onTouchEvent(event);
					if(eventToSelCon){
						return true;
					}
					break;
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_UP:
					if(eventToSelCon){
						return selController.onTouchEvent(event);
					}
			}
		}else{
			eventToSelCon = false;
		}
		
		//Toast.makeText(getContext(),"Line:"+getCharOffsetByThumb(event.getX(),getLineByThumbY(event.getY())),0).show();
		
		int thumb = event.getPointerCount();//thumb count
		if(thumb == 1 && event.getAction() != MotionEvent.ACTION_UP){
			//might a scale start
			//so we must send the event to the two detectors
			
			//this ensure that we send event to the two detectors
			boolean b = mDetector_Basic.onTouchEvent(event);
			boolean c = mDetector_Scale.onTouchEvent(event);
			
			return b||c;
		}else if(thumb == 2){
			//too thumbs now
			//due to scale corrently,we only send to one detector;
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
		//mState.setScrollMaxX(MeasureSpec.getSize(widthMeasureSpec)*3);
		//scroll max y
		mState.setScrollMaxY(getLineCount() * (int)mStyle.getLineHeight() - MeasureSpec.getSize(heightMeasureSpec)/2);
		//call super to apply the size config
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	//---------------------------------------
	
	private float offsetX;
	private int CurrLine;
	private int selSt;
	private int selEd;
	private float tabSize;
	
	/*
	 * getLastOffset():float
	 *
	 * This method is for internal calls
	 * It returns the last offset x cache when drawing
	 * It is used to calculate the selection positions
	 */
	public float getLastOffset(){
		return offsetX;
	}
	
	/*
	 * measureText()
	 *
	 * It is used to measure the text in EditorText
	 * which is using by us from the position {st} to {ed}
	 *
	 * @param st The start of text
	 * @param ed The end of text
	 * @return text measure width
	 */
	public float measureText(int st,int ed){
		return mPaint.measureText(mText.subSequence(st,ed).toString().replace("\t","    "));
	}
	
	@Override
	protected void onDraw(Canvas canvas){
		//call super to draw background.
		//You can set background for our view
		super.onDraw(canvas);
		//make offset x
		offsetX = -mState.getOffsetX();
		//draw the current line's background
		drawCurrLineBackground(canvas);
		//draw line numbers and their background
		//if need(plaease set it in EditorStyle)
		if(mStyle.showLineNumber){
			drawLineNumbers(canvas);
			drawDividerLine(canvas);
		}
		//draw the texts on current screen
		drawText_debug(canvas);
		if(selController != null && getStart() != getEnd()){
			//dispatch controller to draw
			selController.onDraw(canvas);
		}
	}
	
	/*
	 * Draw the text on current screen
	 */
	private void drawText_debug(Canvas canvas){
		mPaint.setColor(mStyle.defaultTextColor);
		//we only draw the text visible
		int i = getFirstVisibleLine();
		int m = getLastVisibleLine();
		selSt = Selection.getSelectionStart(mText);
		selEd = Selection.getSelectionEnd(mText);
		tabSize = 4 * mPaint.measureText(" ");
		mlw = 0;
		for(;i<=m;i++){
			drawLine_debug(mText,i,offsetX,getLineBaseLineOnScreen(i),canvas);
		}
		mState.setScrollMaxX((int)Math.ceil(mlw + mPaint.measureText(Integer.toString(getLineCount())) + 20) - getWidth()/2);
	}
	
	//Draw a text line
	private float mlw;
	private void drawLine_debug(CharSequence s,int li,float x,float y,Canvas c){
		int st = getLineStart(li);
		int en = getLineEnd(li);
		int start = selSt;
		int end = selEd;
		if(true){
			if(start != end && start < en && end > st){
				//selected text
				int left = Math.max(start,st);
				int right = Math.min(end,en);
				float startX = offsetX + mPaint.measureText(mText.subSequence(st,left).toString().replace("\t","    "));
				float endX = startX + mPaint.measureText(mText.subSequence(left,right).toString().replace("\t","    "));
				mPaint.setColor(mStyle.dividerLineColor);
				mPaint.setAlpha(120);
				c.drawRect(startX,getLineTopOnScreen(li),endX,getLineBottomOnScreen(li),mPaint);
				mPaint.setAlpha(255);
				mPaint.setColor(mStyle.defaultTextColor);
			}
			String str = mText.subSequence(st,en).toString().replace("\t","    ");
			c.drawText(str,x,y,mPaint);
			if(st<=start && en >= start && li == CurrLine){
				float xf = measureText(st,start) + x;
				//draw the cursor directly
				drawCursorIfHereIs(start,start-1,xf,li,c);
			}
			if(st<=end && en >= end && getLineByIndex(end) == li){
				float xf = measureText(st,end) + x;
				//draw the cursor directly
				drawCursorIfHereIs(end,end-1,xf,li,c);
			}
			mlw = Math.max(mlw,mPaint.measureText(str));
			return;
		}
		
		//Deprecated
		//It has a low effect
		//And we are going to draw spans
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
	
	//Draw insert cursor with a simple IF
	private void drawCursorIfHereIs(int cursorPos,int index,float offsetX,int line,Canvas c){
		if(cursorPos == index + 1){
			int backup = mPaint.getColor();
			mPaint.setColor(Color.BLACK);
			mPaint.setStrokeWidth(2);
			c.drawLine(offsetX,getLineTopOnScreen(line),offsetX,getLineBottomOnScreen(line),mPaint);
			mPaint.setColor(backup);
		}
	}
	
	//Draw the background of the line you are working with
	private void drawCurrLineBackground(Canvas canvas){
		int charOffset = Selection.getSelectionStart(mText);
		int line = getLineByIndex(charOffset);
		CurrLine = line;
		if(getStart() != getEnd()){
			return;
		}
		mPaint.setColor(mStyle.lineColor);
		float top = getLineTopOnScreen(line);
		float bot = top + mStyle.getLineHeight();
		canvas.drawRect(0,top,getWidth(),bot,mPaint);
	}
	
	//Draw line numbers as well as its background
	private void drawLineNumbers(Canvas canvas){
		int i = getFirstVisibleLine();
		int m = getLastVisibleLine();
		float x = 0;
		float width = mPaint.measureText(Integer.toString(getLineCount()));
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
	
	//Draw the divider line
	private void drawDividerLine(Canvas canvas){
		mPaint.setColor(mStyle.dividerLineColor);
		//5 is the width of divider
		canvas.drawRect(offsetX,0,offsetX+5,getHeight(),mPaint);
		//extra 5 is the right margin
		offsetX += 10;
	}
	
	//Draw the line number background
	private void drawLineNumberBackground(Canvas canvas,float from,float to){
		mPaint.setColor(mStyle.LNBG);
		canvas.drawRect(from,0,to,getHeight(),mPaint);
	}
	
	//Get which line the thumb is
	//-1 for smaller than first line
	//-2 for larger than last line
	public int getLineByThumbY(float thumbY){
		float j = thumbY + getOffsetOnScreen();
		int line = (int)j/(int)mStyle.getLineHeight() + getFirstVisibleLine();
		if(line < 0){
			return -1;
		}else if(line >= getLineCount()){
			return -2;
		}
		return line;
	}
	
	//Get selection start
	public int getStart(){
		return Selection.getSelectionStart(mText);
	}
	
	//Get selection end
	public int getEnd(){
		return Selection.getSelectionEnd(mText);
	}
	
	//Set selection position
	public void select(int i){
		select(i,i);
	}
	
	public void select(int st,int ed){
		Selection.setSelection(mText,st,ed);
	}
	
	//Get the char index under given thumb info
	public int getCharOffsetByThumb(float thumbX,int line){
		if(line == -1){
			line = 0;
		}
		if(line == -2){
			line = getLineCount() - 1;
		}
		float x = thumbX - getLastOffset();
		int st = getLineStart(line);
		int ed = getLineEnd(line);
		int j = st;
		float drawX = 0;
		while(drawX < x && j < ed){
			drawX = measureText(st,j);
			j++;
		}
		if(st != ed && st < j && drawX > x){
			j--;
		}
		return j;
	}
	
	//get the left index of given index
	private int getLeftPosition(int i){
		if(i != 0){
			return i - 1;
		}
		return 0;
	}
	
	//get the right position of given index
	private int getRightPosition(int i){
		if(i != mText.length()){
			return i + 1;
		}
		return mText.length();
	}
	
	//get the up position of given index
	private int getUpPosition(int i){
		int line = getLineByIndex(i);
		if(line == 0){
			return 0;
		}else{
			int currSt = getLineStart(line);
			int lastSt = getLineStart(line - 1);
			int lastEd = getLineEnd(line - 1);
			int offset = i - currSt;
			int n = offset + lastSt;
			if(n > lastEd){
				n = lastEd;
			}
			return n;
		}
	}
	
	//get the down position of given index
	private int getDownPosition(int i){
		int line = getLineByIndex(i);
		if(line >= getLineCount() - 1){
			return mText.length();
		}else{
			int currSt = getLineStart(line);
			int nxtSt = getLineStart(line + 1);
			int nxtEd = getLineEnd(line + 1);
			int offset = i - currSt;
			int n = offset + nxtSt;
			if(n > nxtEd){
				n = nxtEd;
			}
			return n;
		}
	}
	
	//move selection start up
	public void moveSelectionUp(){
		Selection.setSelection(mText,getUpPosition(Selection.getSelectionStart(mText)));
		invalidate();
	}
	
	//move selection start down
	public void moveSelectionDown(){
		Selection.setSelection(mText,getDownPosition(Selection.getSelectionStart(mText)));
		invalidate();
	}
	
	//move slelection start left
	public void moveSelectionLeft(){
		Selection.setSelection(mText,getLeftPosition(Selection.getSelectionStart(mText)));
		invalidate();
	}
	
	//move selection start right
	public void moveSelectionRight(){
		Selection.setSelection(mText,getRightPosition(Selection.getSelectionStart(mText)));
		invalidate();
	}
	
	//create selection modify controller
	public void createSelectionControllerIfNeed(){
		if(selController == null){
			selController = new SelectionController(this);
		}
	}

	@Override
	public void computeScroll() {
		//Override due to scrolling effects
		if(mState.getScroller().computeScrollOffset()){
			invalidate();
		}
		super.computeScroll();
	}
	
	//---------------------------------------
	
	/*
	 * calculateScrollMaxY()
	 *
	 * Internal call
	 * This method send the new scroll max y position to EditorTouch
	 */
	private void calculateScrollMaxY(){
		mState.setScrollMaxY(getLineCount() * ((int)mStyle.getLineHeight()) - getHeight()/2);
	}
	
	/*
	 * getFirstVisibleLine():int
	 *
	 * Get the first line on the screen
	 * @return first line index
	 */
	public int getFirstVisibleLine(){
		int i = mState.getOffsetY()/((int)mStyle.getLineHeight());
		return (i>=0)?i:0;
	}
	
	/*
	 * getLastVisibleLine():int
	 *
	 * Get the last visible line on the screen
	 * @return last line index
	 */
	public int getLastVisibleLine(){
		int l = (int)Math.ceil((mState.getOffsetY()+getHeight())/mStyle.getLineHeight());
		return (l<getLineCount()?l:getLineCount() -1);
	}
	
	/*
	 * getLineBaseLineOnScreen(int):long
	 *
	 * To decrease the float operations
	 * The float is not exact when we testing
	 * and the text will display on wrong position
	 * on the screen when there are about 3 million lines
	 *
	 * @return The base line y for line on canvas
	 */
	public long getLineBaseLineOnScreen(int line){
		return (long)getLineBaseline(line - getFirstVisibleLine())-getOffsetOnScreen();
	}
	
	/*
	 * getLineTopOnScreen(int):long
	 *
	 * The same usage as above method
	 *
	 * @return The top line position on canvas
	 */
	public long getLineTopOnScreen(int line){
		return (long)mStyle.getLineTop(line - getFirstVisibleLine()) - getOffsetOnScreen();
	}
	
	/*
	 * getLineBottomOnScreen(int):long
	 * 
	 * The same usage as above method
	 *
	 * @return The bottom line on canvas
	 */
	public long getLineBottomOnScreen(int line){
		return (long)mStyle.getLineBottom(line - getFirstVisibleLine()) - getOffsetOnScreen();
	}
	
	/*
	 * getOffsetOnScreen():long
	 *
	 * This is a simple helper of getBaseLineOnScreen() and other method
	 * which ends with "OnScreen"
	 * It return the top position of the first line on canvas
	 *
	 * @return The first line offset
	 */
	long getOffsetOnScreen(){
		long offY = mState.getOffsetY();
		long fl = getFirstVisibleLine();
		long newOff = offY - (long)mStyle.getLineHeight()*fl;
		return newOff;
	}
	
	/*
	 * getLineBaseline(int):float
	 *
	 * It is not exact when the line is big
	 *
	 * @return the base line y related to the zero
	 */
	public float getLineBaseline(int line){
		float top = mStyle.top;
		float bottom = mStyle.bottom;
		return mStyle.getLineHeight() * (line + 0.5f) + (top-bottom)/2;
	}
	
	/*
	 * getLineCount():int
	 *
	 * Get how many lines there are
	 * @see LineManager
	 */
	public int getLineCount(){
		return mText.getLineCount();
	}

	/*
	 * getLineStart(int):int
	 * 
	 * With the given line index,we try to
	 * find the start char offset through LineManager
	 * It will spend a long time when you
	 * are trying to get a line that far from 
	 * the lines you have already requested.
	 * The lines you requested long time ago which
	 * are not used in a long time will also spend
	 * a long time.
	 *
	 * @return The first character index of line
	 */
	public int getLineStart(int line){
		//try to not get the wrong start
		//beacause LineManager always return
		//the '\n' as the line start for
		//the lines which are not the first line
		//we would like to prevent it
		int i = mText.getLineStart(line);
		if(line != 0){
			i++;
		}
		return i;
	}

	/*
	 * getLineEnd(int):int
	 * 
	 * The same as getLineStart() except that this returns the end index
	 * @return The end index of line
	 */
	public int getLineEnd(int line){
		return mText.getLineEnd(line);
	}

	/*
	 * Get the line index by char offset
	 */
	public int getLineByIndex(int charOffset){
		//fix the bug
		try{
			if(mText.charAt(charOffset)=='\n' && charOffset != 0){
				charOffset--;
			}
		}catch(Exception e){
		}
		return mText.getLineByIndex(charOffset);
	}

	//---------------------------------------
	
	//this flag is used to tell us whether the delete action is a part of replace
	//so we will not redraw twice when the text is being replaced
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

	/*
	 * isEditable():boolean
	 * @see setEditable(boolean)
	 * @return whether the editor is editable
	 */
	public boolean isEditable(){
		return mEditable;
	}
	
	/*
	 * setEditable(boolean)
	 * Set whether the editor is editable
	 * If not,the selection and input method will not
	 * show and the text can not be selected
	 */
	public void setEditable(boolean editable){
		mEditable = editable;
	}

	@Override
	public boolean onCheckIsTextEditor() {
		//Override
		return isEditable() && isEnabled();
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT;
		//put necessary information for input method
		outAttrs.initialSelStart = Selection.getSelectionStart(mText);
		outAttrs.initialSelEnd = Selection.getSelectionEnd(mText);
		//when we create a connection
		//we reset the batch edit state
		mText.resetBatchEdit();
		return onCheckIsTextEditor() ? (conn = new EditorInputConnection(this)) : null;
	}
	
	//---------------------------------------
	
	public class EditorStyle{
		
		//These are colors
		private int defaultTextColor = Color.BLACK;
		private int lineColor = 0x66ec407a;
		private int lineNumberColor = 0xff3f51b5;
		private int dividerLineColor = 0xff3f51b5;
		private int LNBG = 0xeeeeeeee;
		private int handleColor = 0xed3f51b5;
		
		//Features
		private boolean showLineNumber = true;
		private boolean distsncePixelOrDouble;
		private float distance;
		private float top,bottom,ascent,descent;
		private int lnGravity;
		
		private EditorStyle(){
			distsncePixelOrDouble = true;
			distance = 0;
		}
		
		/*
		 * Set a typeface for editor
		 */
		public void setTypeface(Typeface typeface){
			if(typeface == null){
				typeface = Typeface.MONOSPACE;
			}
			mPaint.setTypeface(typeface);
			calculateScrollMaxY();
			invalidate();
		}
		
		//Getter
		public Typeface getTypeface(){
			return mPaint.getTypeface();
		}
		
		/*
		 * Set a new text size for editor
		 * Your scrolling position will not be changed!
		 * It means that the displsy that user can see
		 * will change a lot if the value 
		 * is far from the size before
		 */
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
		
		//Getter
		public float getTextSize(){
			return mPaint.getTextSize();
		}
		
		/*
		 * Set text scale x
		 */
		public void setTextScaleX(float scaleX){
			mPaint.setTextScaleX(scaleX);
			invalidate();
		}
		
		//Getter
		public float getTextScaleX(){
			return mPaint.getTextScaleX();
		}
		
		/*
		 * Set text skew x
		 */
		public float getTextSkewX(){
			return mPaint.getTextSkewX();
		}
		
		//Getter
		public void setTextSkewX(float skewX){
			mPaint.setTextSkewX(skewX);
			invalidate();
		}
		
		/*
		 * Get line height of each line including line distance
		 * It is a simple sum of real height and distance
		 * If you want to get the real height of current text size
		 * please call getLineRealHeight()
		 */
		public float getLineHeight(){
			return (getLineRealHeight() + getLineDistancePixel());
		}
		
		/*
		 * Get line top line absolutely
		 * It will be not exactly if the number is large
		 * (Important!)
		 */
		public float getLineTop(int line){
			return getLineHeight() * line;
		}
		
		/*
		 * Get line bottom absolutely
		 * It will be not exactly if the number is large
		 * (Important!)
		 */
		public float getLineBottom(int line){
			return getLineTop(line) + getLineHeight();
		}
		
		/*
		 * Get the real line height
		 * It only depends on the text size
		 * and the typeface you set
		 */
		public float getLineRealHeight(){
			return (float)Math.ceil(descent) + (float)Math.ceil(ascent);
		}
		
		/*
		 * Get the line distance in pixel
		 */
		public float getLineDistancePixel(){
			return (distsncePixelOrDouble ? distance : getLineRealHeight() * distance);
		}
		
		/*
		 * Set the line distance with a determinated value
		 */
		public void setLineDistancePixel(float pixel){
			if(pixel < 0){
				throw new IllegalArgumentException("under zero is not allowed");
			}
			distsncePixelOrDouble = true;
			distance = pixel;
			calculateScrollMaxY();
			invalidate();
		}
		
		/*
		 * Set a line distance which 
		 * depends on the text size
		 * it is d*getRealLineHeight()
		 * And it will change if text size changes
		 */
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
		
		/*
		 * Set whether editor should show line number and divider
		 */
		public void setShowLineNumber(boolean s){
			//decrease useless calls
			if(s == showLineNumber){
				return;
			}
			showLineNumber = s;
			invalidate();
		}
		
		/*
		 * Set the default text color
		 * as there is no highlighter or the highlighter
		 * did not set a span for some regions
		 */
		public void setDefaultTextColor(int color){
			this.defaultTextColor = color;
			invalidate();
		}
		
		//Getter
		public int getDefaultTextColor(){
			return this.defaultTextColor;
		}
		
		/*
		 * Set line number text color
		 */
		public void setLineNumberColor(int c){
			lineNumberColor=c;
			invalidate();
		}
		
		//Getter
		public int getLineNumberColor(){
			return lineNumberColor;
		}
		
		/*
		 * Set the color of divider line
		 * between the line number rectange and
		 * text editing rectange
		 */
		public void setDividerColor(int c){
			dividerLineColor=c;
			invalidate();
		}
		
		//Getter
		public int getDividerColor(){
			return dividerLineColor;
		}
		
		/*
		 * Set the background color of line number rectange
		 */
		public void setLineNumberBackground(int c){
			LNBG =c;
			invalidate();
		}
		
		//Getter
		public int getLineNumberBackground(){
			return LNBG;
		}
		
		/*
		 * Set the background color of the line you are working
		 * with.
		 */
		public void setCurrentLineColor(int c){
			lineColor = c;
			invalidate();
		}
		
		//Getter
		public int getCurrentLineColor(){
			return lineColor;
		}
		
		public void setHandleColor(int c){
			handleColor = c;
			if(selController != null){
				selController.getLeftHandle().setHandleColor(c);
				selController.getRightHandle().setHandleColor(c);
				invalidate();
			}
		}
		
		public int getHandleColor(){
			return handleColor;
		}
		
		/*
		 * Set the line number gravity
		 *
		 * Note that we only support:
		 * Gravity.CENTER
		 * Gravity.LEFT
		 * Gravity.RIGHT
		 * And any mask is not supported.
		 * The Gravity.CENTER_HORIZONTAL will be
		 * replaced with Gravity.CENTER automatically
		 * by calling this method.
		 * It is better to use Gravity.CENTER if you use
		 * Gravity.CENTER_HORIZONTAL
		 */
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
					throw new IllegalArgumentException("Given gravity mask not supported");
			}
		}
		
		//Getter
		//Note that you will never get other values
		//except CENTER,LEFT,RIGHT in Gravity
		public int getLineNumberFravity(){
			return lnGravity;
		}
	}
	
}
